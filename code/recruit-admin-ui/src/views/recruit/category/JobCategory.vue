<template>
  <div class="category-page">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd()">
        <el-icon><Plus /></el-icon> 新增类别
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="treeList"
        row-key="categoryId"
        stripe
        size="small"
        style="width: 100%;"
        default-expand-all
      >
        <el-table-column prop="categoryName" label="类别名称" min-width="180" />
        <el-table-column prop="orderNum" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleAdd(row)">
              添加子类
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
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" size="default">
        <el-form-item label="类别名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入类别名称" maxlength="30" />
        </el-form-item>
        <el-form-item label="类别编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="请输入类别编码（如：SOFTWARE）" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="orderNum">
          <el-input-number v-model="form.orderNum" :min="0" :max="999" style="width: 140px;" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="form.parentId" label="父类别">
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
import request from '@/utils/request';

const loading = ref(false);
const treeList = ref([]);
const dialogVisible = ref(false);
const dialogTitle = ref('新增类别');
const submitLoading = ref(false);
const formRef = ref(null);
const parentName = ref('');

const isEdit = ref(false);
const editId = ref(null);

const form = reactive({
  categoryName: '',
  categoryCode: '',
  orderNum: 0,
  status: 1,
  parentId: null,
});

const rules = {
  categoryName: [{ required: true, message: '请输入类别名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请输入类别编码', trigger: 'blur' }],
};

// 将平铺列表构建为树形数据
function buildTree(list) {
  const map = {};
  const tree = [];
  list.forEach(item => {
    map[item.categoryId] = { ...item, children: item.children || [] };
  });
  list.forEach(item => {
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(map[item.categoryId]);
    } else if (!item.parentId) {
      tree.push(map[item.categoryId]);
    }
  });
  return tree;
}

// 查找父类别名称
function findParentName(parentId) {
  const find = (nodes) => {
    for (const n of nodes) {
      if (n.categoryId === parentId) return n.categoryName;
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
    const res = await request.get('/jobCategory/list');
    const raw = res.data || [];
    treeList.value = buildTree(raw);
  } catch {
    // ignore
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  form.categoryName = '';
  form.categoryCode = '';
  form.orderNum = 0;
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
    form.parentId = parentRow.categoryId;
    parentName.value = parentRow.categoryName;
  }
  dialogTitle.value = parentRow ? `添加「${parentRow.categoryName}」的子类别` : '新增类别';
  dialogVisible.value = true;
}

function handleEdit(row) {
  resetForm();
  isEdit.value = true;
  editId.value = row.categoryId;
  form.categoryName = row.categoryName;
  form.categoryCode = row.categoryCode || '';
  form.orderNum = row.orderNum ?? 0;
  form.status = row.status ?? 1;
  form.parentId = row.parentId || null;
  if (row.parentId) {
    parentName.value = findParentName(row.parentId);
  }
  dialogTitle.value = '编辑类别';
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitLoading.value = true;
  try {
    if (isEdit.value) {
      await request.put('/jobCategory', { categoryId: editId.value, ...form });
      ElMessage.success('编辑成功');
    } else {
      await request.post('/jobCategory', { ...form });
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
    // 检查是否有子类别（从 treeList 中看）
    const hasChildren = treeList.value.some(item => item.parentId === row.categoryId);
    if (hasChildren) {
      ElMessage.warning('该类别下存在子类别，无法删除');
      return;
    }
    await ElMessageBox.confirm(`确认删除类别「${row.categoryName}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await request.delete(`/jobCategory/${row.categoryId}`);
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
.category-page {
  max-width: 1000px;
  margin: 0 auto;
}

.toolbar {
  margin-bottom: 12px;
}
</style>
