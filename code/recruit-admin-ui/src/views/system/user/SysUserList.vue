<template>
  <div class="user-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-bar-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名/昵称/手机号" clearable style="width: 200px;" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="启用" :value="'0'" />
            <el-option label="禁用" :value="'1'" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 160px;">
            <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon> 新增账号
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
        <el-table-column prop="userName" label="用户名" min-width="120" />
        <el-table-column prop="nickName" label="昵称" min-width="100" />
        <el-table-column prop="deptName" label="部门" min-width="120" />
        <el-table-column prop="phonenumber" label="手机号" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag
              :type="row.status === '0' ? 'success' : 'info'"
              size="small"
              :style="row.userName === 'AT-admin' ? 'cursor: default;' : 'cursor: pointer;'"
              @click="row.userName !== 'AT-admin' && handleToggleStatus(row)"
            >
              {{ row.status === '0' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ (row.roleNames || []).join(' / ') || '未分配' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.userName !== 'AT-admin'" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link size="small" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button
              v-if="row.userName !== 'AT-admin' && row.status === '0'"
              type="danger"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              禁用
            </el-button>
            <el-button
              v-if="row.userName !== 'AT-admin' && row.status !== '0'"
              type="success"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              启用
            </el-button>
            <el-button v-if="row.userName !== 'AT-admin'" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" size="default">
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入用户名" :disabled="isEdit" maxlength="30" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password maxlength="32" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickName">
          <el-input v-model="form.nickName" placeholder="请输入昵称" maxlength="30" />
        </el-form-item>
        <el-form-item label="部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="请选择部门" style="width: 100%;">
            <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phonenumber">
          <el-input v-model="form.phonenumber" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="100" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" placeholder="请选择角色" multiple style="width: 100%;">
            <el-option v-for="r in roleOptions" :key="r.roleId" :label="r.roleName" :value="r.roleId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="'0'" :inactive-value="'1'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="420px" :close-on-click-modal="false">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px" size="default">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" placeholder="请输入新密码" show-password maxlength="32" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="handlePwdSubmit">确定</el-button>
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
const deptOptions = ref([]);
const roleOptions = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  deptId: '',
});

// 新增/编辑弹窗
const dialogVisible = ref(false);
const dialogTitle = ref('新增账号');
const submitLoading = ref(false);
const formRef = ref(null);
const isEdit = ref(false);
const editId = ref(null);

const form = reactive({
  userName: '',
  password: '',
  nickName: '',
  deptId: null,
  phonenumber: '',
  email: '',
  roleIds: [],
  status: '0',
});

const rules = {
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择部门', trigger: 'change' }],
  phonenumber: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
};

// 重置密码弹窗
const pwdDialogVisible = ref(false);
const pwdFormRef = ref(null);
const pwdLoading = ref(false);
const pwdTargetId = ref(null);
const pwdForm = reactive({ password: '' });
const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码不少于6位', trigger: 'blur' },
  ],
};

// 获取用户列表
async function fetchList() {
  loading.value = true;
  try {
    const res = await systemRequest.get('/user/list', { params: { ...query } });
    list.value = res.data?.rows || [];
    total.value = res.data?.total || 0;
  } catch {
    // ignore
  } finally {
    loading.value = false;
  }
}

// 获取部门列表
async function fetchDeptOptions() {
  try {
    const res = await systemRequest.get('/dept/list');
    const raw = res.data || [];
    // 展平树形结构
    function flatten(nodes) {
      const result = [];
      for (const n of nodes || []) {
        result.push(n);
        if (n.children) result.push(...flatten(n.children));
      }
      return result;
    }
    deptOptions.value = flatten(raw);
  } catch { /* ignore */ }
}

// 获取角色列表
async function fetchRoleOptions() {
  try {
    const res = await systemRequest.get('/role/list', { params: { pageNum: 1, pageSize: 100 } });
    roleOptions.value = res.data?.rows || [];
  } catch { /* ignore */ }
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.keyword = '';
  query.status = '';
  query.deptId = '';
  query.pageNum = 1;
  fetchList();
}

function resetForm() {
  form.userName = '';
  form.password = '';
  form.nickName = '';
  form.deptId = null;
  form.phonenumber = '';
  form.email = '';
  form.roleIds = [];
  form.status = '0';
  isEdit.value = false;
  editId.value = null;
  formRef.value?.resetFields();
}

function handleAdd() {
  resetForm();
  // 新增时密码必填
  rules.password = [{ required: true, message: '请输入密码', trigger: 'blur' }];
  dialogTitle.value = '新增账号';
  dialogVisible.value = true;
}

function handleEdit(row) {
  resetForm();
  isEdit.value = true;
  editId.value = row.userId;
  form.userName = row.userName;
  form.nickName = row.nickName;
  form.deptId = row.deptId;
  form.phonenumber = row.phonenumber || '';
  form.email = row.email || '';
  form.roleIds = row.roleIds || [];
  form.status = row.status;
  // 编辑时密码非必填
  rules.password = [];
  dialogTitle.value = '编辑账号';
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitLoading.value = true;
  try {
    if (isEdit.value) {
      await systemRequest.put('/user', { userId: editId.value, ...form });
      ElMessage.success('编辑成功');
    } else {
      await systemRequest.post('/user', { ...form });
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

// 重置密码
function handleResetPwd(row) {
  pwdTargetId.value = row.userId;
  pwdForm.password = '';
  pwdDialogVisible.value = true;
}

async function handlePwdSubmit() {
  const valid = await pwdFormRef.value.validate().catch(() => false);
  if (!valid) return;

  pwdLoading.value = true;
  try {
    await systemRequest.put(`/user/resetPwd`, { userId: pwdTargetId.value, password: pwdForm.password });
    ElMessage.success('密码重置成功');
    pwdDialogVisible.value = false;
  } catch {
    // ignore
  } finally {
    pwdLoading.value = false;
  }
}

// 启禁用
async function handleToggleStatus(row) {
  const action = row.status === '0' ? '禁用' : '启用';
  try {
    await ElMessageBox.confirm(`确认${action}账号「${row.userName}」吗？`, '确认操作', {
      confirmButtonText: `确认${action}`,
      cancelButtonText: '取消',
      type: 'warning',
    });
    const newStatus = row.status === '0' ? '1' : '0';
    await systemRequest.put('/user', { userId: row.userId, status: newStatus });
    ElMessage.success(`${action}成功`);
    fetchList();
  } catch {
    // 取消或错误
  }
}

// 删除
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除账号「${row.userName}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await systemRequest.delete(`/user/${row.userId}`);
    ElMessage.success('删除成功');
    fetchList();
  } catch {
    // 取消或错误
  }
}

onMounted(() => {
  fetchDeptOptions();
  fetchRoleOptions();
  fetchList();
});
</script>

<style scoped>
.user-list-page {
  max-width: 1400px;
  margin: 0 auto;
}

.search-bar-card {
  margin-bottom: 12px;
}

.search-bar-card :deep(.el-card__body) {
  padding-bottom: 0;
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
