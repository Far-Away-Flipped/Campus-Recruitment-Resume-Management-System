<template>
  <div class="template-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-bar-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="模板名称/编码" clearable style="width: 200px;" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="query.channel" placeholder="全部" clearable style="width: 130px;">
            <el-option label="站内信" value="SITE" />
            <el-option label="短信" value="SMS" />
            <el-option label="邮件" value="EMAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 110px;">
            <el-option label="启用" :value="'0'" />
            <el-option label="禁用" :value="'1'" />
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
        <el-icon><Plus /></el-icon> 新增模板
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
        <el-table-column prop="templateCode" label="模板编码" min-width="140" />
        <el-table-column prop="templateName" label="模板名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="渠道" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="channelTagType(row.channel)">
              {{ channelLabel(row.channel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '0' ? 'success' : 'danger'" size="small">
              {{ row.status === '0' ? '启用' : '禁用' }}
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
          v-model:current-page="query.page"
          v-model:page-size="query.size"
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
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" size="default">
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="form.templateCode" placeholder="如：INTERVIEW_NOTICE" :disabled="isEdit" maxlength="50" />
        </el-form-item>
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="form.templateName" placeholder="如：面试通知模板" maxlength="50" />
        </el-form-item>
        <el-form-item label="发送渠道" prop="channel">
          <el-select v-model="form.channel" placeholder="请选择发送渠道" style="width: 100%;">
            <el-option label="站内信" value="SITE" />
            <el-option label="短信" value="SMS" />
            <el-option label="邮件" value="EMAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="'0'" :inactive-value="'1'" />
        </el-form-item>
        <el-form-item label="模板内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入模板内容，可使用下方变量占位..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="可用变量">
          <div class="variable-hints">
            <el-tag
              v-for="v in availableVariables"
              :key="v"
              class="variable-tag"
              size="small"
              type="info"
              @click="insertVariable(v)"
            >
              {{ v }}
            </el-tag>
          </div>
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
const list = ref([]);
const total = ref(0);

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  channel: '',
  status: '',
});

// 弹窗
const dialogVisible = ref(false);
const dialogTitle = ref('新增模板');
const submitLoading = ref(false);
const formRef = ref(null);
const isEdit = ref(false);
const editId = ref(null);

const form = reactive({
  templateCode: '',
  templateName: '',
  channel: '',
  status: '0',
  content: '',
});

const rules = {
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  channel: [{ required: true, message: '请选择发送渠道', trigger: 'change' }],
  content: [{ required: true, message: '请输入模板内容', trigger: 'blur' }],
};

// 可用变量列表
const availableVariables = [
  '{学生姓名}',
  '{岗位名称}',
  '{公司名称}',
  '{面试时间}',
  '{面试地点}',
  '{HR姓名}',
  '{联系电话}',
  '{联系邮箱}',
  '{截止日期}',
  '{当前日期}',
];

const channelMap = { SITE: '站内信', SMS: '短信', EMAIL: '邮件' };
const channelTagMap = { SITE: 'success', SMS: 'warning', EMAIL: '' };

function channelLabel(c) {
  return channelMap[c] || c;
}

function channelTagType(c) {
  return channelTagMap[c] || 'info';
}

// 点击变量标签插入到内容
function insertVariable(variable) {
  form.content = (form.content || '') + variable;
}

async function fetchList() {
  loading.value = true;
  try {
    const res = await request.get('/notifyTemplate/list', { params: { ...query } });
    const data = res.data || {};
    list.value = data.rows || [];
    total.value = data.total || 0;
  } catch {
    // ignore
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.page = 1;
  fetchList();
}

function handleReset() {
  query.keyword = '';
  query.channel = '';
  query.status = '';
  query.page = 1;
  fetchList();
}

function resetForm() {
  form.templateCode = '';
  form.templateName = '';
  form.channel = '';
  form.status = '0';
  form.content = '';
  isEdit.value = false;
  editId.value = null;
  formRef.value?.resetFields();
}

function handleAdd() {
  resetForm();
  dialogTitle.value = '新增模板';
  dialogVisible.value = true;
}

function handleEdit(row) {
  resetForm();
  isEdit.value = true;
  editId.value = row.id;
  form.templateCode = row.templateCode;
  form.templateName = row.templateName;
  form.channel = row.channel;
  form.status = row.status ?? '0';
  form.content = row.content || '';
  dialogTitle.value = '编辑模板';
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  submitLoading.value = true;
  try {
    if (isEdit.value) {
      await request.put('/notifyTemplate', { id: editId.value, ...form });
      ElMessage.success('编辑成功');
    } else {
      await request.post('/notifyTemplate', { ...form });
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
    await ElMessageBox.confirm(`确认删除模板「${row.templateName}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await request.delete('/notifyTemplate/' + row.id);
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
.template-page {
  max-width: 1300px;
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

/* 变量提示 */
.variable-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.variable-tag {
  cursor: pointer;
  transition: transform 0.15s;
}

.variable-tag:hover {
  transform: scale(1.05);
  opacity: 0.85;
}
</style>
