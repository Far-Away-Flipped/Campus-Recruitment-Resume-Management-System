<template>
  <div class="resume-list-page">
    <!-- 筛选区 -->
    <el-card shadow="never" class="search-bar-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="岗位">
          <el-select v-model="query.jobId" placeholder="全部岗位" clearable filterable style="width: 180px;">
            <el-option v-for="j in jobOptions" :key="j.jobId" :label="j.title" :value="j.jobId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px;">
            <el-option label="待筛选" value="PENDING_SCREEN" />
            <el-option label="已通过" value="SCREEN_PASSED" />
            <el-option label="已淘汰" value="ELIMINATED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索姓名/学校" clearable style="width: 180px;" />
        </el-form-item>
        <el-form-item label="学校">
          <el-input v-model="query.school" placeholder="学校名称" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="query.major" placeholder="专业名称" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="学历">
          <el-select v-model="query.degree" placeholder="全部" clearable style="width: 120px;">
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>
        <el-form-item label="投递日期">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 260px;"
          />
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
      <el-button
        type="success"
        :disabled="selectedIds.length === 0"
        @click="handleBatchPass"
      >
        批量通过
      </el-button>
      <el-button
        type="danger"
        :disabled="selectedIds.length === 0"
        @click="handleBatchEliminate"
      >
        批量淘汰
      </el-button>
      <el-button type="warning" plain @click="handleExport">
        <el-icon><Download /></el-icon> 导出Excel
      </el-button>
    </div>

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="list"
        stripe
        size="small"
        style="width: 100%;"
        @selection-change="handleSelectionChange"
        @row-click="handleRowClick"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="studentName" label="姓名" min-width="90" />
        <el-table-column prop="school" label="学校" min-width="140" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="140" show-overflow-tooltip />
        <el-table-column prop="degree" label="学历" width="80" />
        <el-table-column prop="applyTime" label="投递时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="$router.push(`/recruit/resumes/${row.applicationId}`)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_SCREEN'"
              type="success"
              link
              size="small"
              @click.stop="handleSinglePass(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_SCREEN'"
              type="danger"
              link
              size="small"
              @click.stop="handleSingleEliminate(row)"
            >
              淘汰
            </el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import axios from 'axios';

const router = useRouter();
const loading = ref(false);
const list = ref([]);
const total = ref(0);
const selectedIds = ref([]);
const jobOptions = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  jobId: '',
  status: '',
  keyword: '',
  school: '',
  major: '',
  degree: '',
  dateRange: null,
});

const statusMap = { PENDING_SCREEN: '待筛选', SCREEN_PASSED: '已通过', ELIMINATED: '已淘汰' };
const statusTagMap = { PENDING_SCREEN: 'warning', SCREEN_PASSED: 'success', ELIMINATED: 'danger' };

function statusLabel(s) {
  return statusMap[s] || s;
}
function statusTagType(s) {
  return statusTagMap[s] || 'info';
}

async function fetchList() {
  loading.value = true;
  try {
    const params = { ...query };
    if (params.dateRange && params.dateRange.length === 2) {
      params.startDate = params.dateRange[0];
      params.endDate = params.dateRange[1];
    }
    delete params.dateRange;
    const res = await request.get('/resumes/list', { params });
    list.value = res.data?.rows || [];
    total.value = res.data?.total || 0;
  } catch { /* ignore */ } finally {
    loading.value = false;
  }
}

async function fetchJobOptions() {
  try {
    const res = await request.get('/jobs/list', { params: { pageNum: 1, pageSize: 200 } });
    jobOptions.value = (res.data?.rows || []).filter(j => j.status === 'PUBLISHED');
  } catch { /* ignore */ }
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.jobId = '';
  query.status = '';
  query.keyword = '';
  query.school = '';
  query.major = '';
  query.degree = '';
  query.dateRange = null;
  query.pageNum = 1;
  fetchList();
}

function handleSelectionChange(rows) {
  selectedIds.value = rows.map(r => r.applicationId);
}

function handleRowClick(row) {
  router.push(`/recruit/resumes/${row.applicationId}`);
}

async function handleSinglePass(row) {
  try {
    await ElMessageBox.confirm(`确认通过「${row.studentName}」的简历筛选吗？`, '确认操作', {
      confirmButtonText: '确认通过',
      type: 'success',
    });
    await request.put(`/resumes/${row.applicationId}/screen-pass`);
    ElMessage.success('已通过');
    fetchList();
  } catch { /* ignore */ }
}

async function handleSingleEliminate(row) {
  try {
    await ElMessageBox.confirm(`确认淘汰「${row.studentName}」的简历吗？`, '确认操作', {
      confirmButtonText: '确认淘汰',
      type: 'warning',
    });
    await request.put(`/resumes/${row.applicationId}/screen-eliminate`);
    ElMessage.success('已淘汰');
    fetchList();
  } catch { /* ignore */ }
}

async function handleBatchPass() {
  try {
    await ElMessageBox.confirm(`确认批量通过选中的 ${selectedIds.value.length} 份简历吗？`, '批量操作确认', {
      confirmButtonText: '确认通过',
      type: 'success',
    });
    await request.post('/resumes/batch-screen', { applicationIds: selectedIds.value, action: 'pass' });
    ElMessage.success(`已通过 ${selectedIds.value.length} 份简历`);
    selectedIds.value = [];
    fetchList();
  } catch { /* ignore */ }
}

async function handleBatchEliminate() {
  try {
    await ElMessageBox.confirm(`确认批量淘汰选中的 ${selectedIds.value.length} 份简历吗？`, '批量操作确认', {
      confirmButtonText: '确认淘汰',
      type: 'warning',
    });
    await request.post('/resumes/batch-screen', { applicationIds: selectedIds.value, action: 'eliminate' });
    ElMessage.success(`已淘汰 ${selectedIds.value.length} 份简历`);
    selectedIds.value = [];
    fetchList();
  } catch { /* ignore */ }
}

async function handleExport() {
  try {
    // 1. 组装筛选参数
    const params = { ...query };
    if (params.dateRange && params.dateRange.length === 2) {
      params.startDate = params.dateRange[0];
      params.endDate = params.dateRange[1];
    }
    delete params.dateRange;

    // 2. 先按筛选条件拉取全量 ID（复用项目 request，不走 blob 所以安全）
    const allRes = await request.get('/resumes/list', { params: { ...params, pageNum: 1, pageSize: 10000 } });
    const allIds = (allRes.data?.rows || []).map(r => r.applicationId);
    if (allIds.length === 0) {
      ElMessage.warning('没有可导出的数据');
      return;
    }

    // 3. 用原生 axios 发起 POST /export（避开 request.js 响应拦截器）
    const token = localStorage.getItem('admin_token');
    const exportRes = await axios.post('/api/admin/resumes/export',
      { applicationIds: allIds },
      { headers: token ? { Authorization: `Bearer ${token}` } : {} }
    );
    // exportRes.data 是已解析的 JSON: { code: 200, msg: "导出成功", data: "E:\\Temp\\..." }

    if (exportRes.data?.code !== 200) {
      ElMessage.error(exportRes.data?.msg || '导出失败');
      return;
    }

    const filePath = exportRes.data?.data;
    if (!filePath) {
      ElMessage.error('导出文件路径为空');
      return;
    }

    // 4. 用原生 axios 以 blob 方式下载文件（带 auth header，避开 request.js 拦截器）
    const downloadUrl = `/api/admin/resumes/export/download?path=${encodeURIComponent(filePath)}`;
    const blobRes = await axios.get(downloadUrl,
      { responseType: 'blob',
        headers: token ? { Authorization: `Bearer ${token}` } : {} }
    );

    // 创建临时 URL 触发浏览器原生下载
    const url = window.URL.createObjectURL(blobRes.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = '';  // 让服务器 Content-Disposition 决定文件名
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    window.URL.revokeObjectURL(url);

    ElMessage.success('导出成功');
  } catch {
    ElMessage.error('导出失败，请重试');
  }
}

onMounted(() => {
  fetchJobOptions();
  fetchList();
});
</script>

<style scoped>
.resume-list-page {
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
  display: flex;
  gap: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
