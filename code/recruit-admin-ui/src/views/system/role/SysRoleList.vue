<template>
  <div class="role-list-page">
    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 新增角色
      </el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="list"
        stripe
        size="small"
        style="width: 100%;"
      >
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="roleKey" label="权限字符" min-width="140" />
        <el-table-column prop="roleSort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="fetchList"
          @current-change="fetchList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="default">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="权限字符" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如：admin、hr" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="roleSort">
          <el-input-number v-model="form.roleSort" :min="0" :max="999" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注（可选）"
            maxlength="200"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import systemRequest from '@/utils/systemRequest';

const loading = ref(false);
const list = ref([]);
const total = ref(0);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
});

// 弹窗
const dialogVisible = ref(false);
const dialogTitle = ref('新增角色');
const submitLoading = ref(false);
const formRef = ref(null);
const isEdit = ref(false);
const editId = ref(null);

const form = reactive({
  roleName: '',
  roleKey: '',
  roleSort: 0,
  status: 1,
  remark: '',
});

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入权限字符', trigger: 'blur' }],
};

async function fetchList() {
  loading.value = true;
  try {
    const res = await systemRequest.get('/role/list', { params: { ...query } });
    list.value = res.data?.rows || [];
    total.value = res.data?.total || 0;
  } catch {
    // ignore
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.roleName = '';
  form.roleKey = '';
  form.roleSort = 0;
  form.status = 1;
  form.remark = '';
  isEdit.value = false;
  editId.value = null;
  formRef.value?.resetFields();
}

function handleAdd() {
  resetForm();
  dialogTitle.value = '新增角色';
  dialogVisible.value = true;
}

function handleEdit(row) {
  resetForm();
  isEdit.value = true;
  editId.value = row.roleId;
  form.roleName = row.roleName;
  form.roleKey = row.roleKey;
  form.roleSort = row.roleSort ?? 0;
  form.status = row.status ?? 1;
  form.remark = row.remark || '';
  dialogTitle.value = '编辑角色';
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitLoading.value = true;
  try {
    if (isEdit.value) {
      await systemRequest.put('/role', { roleId: editId.value, ...form });
      ElMessage.success('编辑成功');
    } else {
      await systemRequest.post('/role', { ...form });
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    fetchList();
  } catch {
    // ignore
  } finally {
    submitLoading.value = false;
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.roleName}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await systemRequest.delete(`/role/${row.roleId}`);
    ElMessage.success('删除成功');
    fetchList();
  } catch {
    // 取消或错误
  }
}

onMounted(() => {
  fetchList();
});
</script>

<style scoped>
.role-list-page {
  max-width: 1000px;
  margin: 0 auto;
}

.toolbar {
  margin-bottom: 12px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
