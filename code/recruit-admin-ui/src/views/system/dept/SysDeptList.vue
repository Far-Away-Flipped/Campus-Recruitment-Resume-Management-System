<template>
  <div class="dept-list-page">
    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd()">
        <el-icon><Plus /></el-icon> 新增部门
      </el-button>
    </div>

    <!-- 树形表格 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="treeList"
        row-key="deptId"
        stripe
        size="small"
        style="width: 100%;"
        default-expand-all
      >
        <el-table-column prop="deptName" label="部门名称" min-width="180" />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleAdd(row)">
              添加子部门
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="default">
        <el-form-item label="部门名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="请输入部门名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="form.leader" placeholder="请输入负责人姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="form.parentId" label="上级部门">
          <el-input :value="parentName" disabled />
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
const treeList = ref([]);
const flatList = ref([]);

// 弹窗
const dialogVisible = ref(false);
const dialogTitle = ref('新增部门');
const submitLoading = ref(false);
const formRef = ref(null);
const isEdit = ref(false);
const editId = ref(null);
const parentName = ref('');

const form = reactive({
  deptName: '',
  sortOrder: 0,
  leader: '',
  phone: '',
  status: 1,
  parentId: null,
});

const rules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
};

// 构建树形数据
function buildTree(list) {
  const map = {};
  const tree = [];
  list.forEach(item => {
    map[item.deptId] = { ...item, children: item.children || [] };
  });
  list.forEach(item => {
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(map[item.deptId]);
    } else if (!item.parentId) {
      tree.push(map[item.deptId]);
    }
  });
  return tree;
}

// 查找上级部门名称
function findParentName(parentId) {
  const find = (nodes) => {
    for (const n of nodes) {
      if (n.deptId === parentId) return n.deptName;
      if (n.children?.length) {
        const r = find(n.children);
        if (r) return r;
      }
    }
    return null;
  };
  return find(treeList.value) || '';
}

async function fetchList() {
  loading.value = true;
  try {
    const res = await systemRequest.get('/dept/list');
    const raw = res.data || [];
    flatList.value = raw;
    treeList.value = buildTree(raw);
  } catch {
    // ignore
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.deptName = '';
  form.sortOrder = 0;
  form.leader = '';
  form.phone = '';
  form.status = 1;
  form.parentId = null;
  parentName.value = '';
  isEdit.value = false;
  editId.value = null;
  formRef.value?.resetFields();
}

function handleAdd(parentRow) {
  resetForm();
  if (parentRow) {
    form.parentId = parentRow.deptId;
    parentName.value = parentRow.deptName;
  }
  dialogTitle.value = parentRow ? `添加「${parentRow.deptName}」的子部门` : '新增部门';
  dialogVisible.value = true;
}

function handleEdit(row) {
  resetForm();
  isEdit.value = true;
  editId.value = row.deptId;
  form.deptName = row.deptName;
  form.sortOrder = row.sortOrder ?? 0;
  form.leader = row.leader || '';
  form.phone = row.phone || '';
  form.status = row.status ?? 1;
  form.parentId = row.parentId || null;
  if (row.parentId) {
    parentName.value = findParentName(row.parentId);
  }
  dialogTitle.value = '编辑部门';
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitLoading.value = true;
  try {
    if (isEdit.value) {
      await systemRequest.put('/dept', { deptId: editId.value, ...form });
      ElMessage.success('编辑成功');
    } else {
      await systemRequest.post('/dept', { ...form });
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
    // 检查是否有子部门
    const hasChildren = flatList.value.some(item => item.parentId === row.deptId);
    if (hasChildren) {
      ElMessage.warning('该部门下存在子部门，无法删除');
      return;
    }
    await ElMessageBox.confirm(`确认删除部门「${row.deptName}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await systemRequest.delete(`/dept/${row.deptId}`);
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
.dept-list-page {
  max-width: 1200px;
  margin: 0 auto;
}

.toolbar {
  margin-bottom: 12px;
}
</style>
