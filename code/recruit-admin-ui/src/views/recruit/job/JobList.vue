<template>
  <div class="job-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-bar-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="关键词">
          <el-input v-model="query.title" placeholder="岗位名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px;">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="已下架" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="query.deptId" placeholder="全部" clearable style="width: 160px;">
            <el-option v-for="d in deptList" :key="d.deptId" :label="d.deptName" :value="d.deptId" />
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
      <el-button type="primary" @click="$router.push('/recruit/jobs/create')">
        <el-icon><Plus /></el-icon> 新增岗位
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
        @sort-change="handleSortChange"
      >
        <el-table-column prop="categoryName" label="岗位分类" min-width="120" sortable="custom" />
        <el-table-column prop="title" label="岗位名称" min-width="160" show-overflow-tooltip sortable="custom" />
        <el-table-column prop="deptName" label="部门" min-width="120" sortable="custom" />
        <el-table-column prop="location" label="工作地点" width="160" sortable="custom">
          <template #default="{ row }">{{ formatLoc(row.location) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止日期" width="120" sortable="custom" />
        <el-table-column prop="applicationCount" label="投递数" width="90" align="center" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="$router.push(`/recruit/jobs/${row.jobId}/edit`)">
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'DRAFT' || row.status === 'CLOSED'"
              type="success"
              link
              size="small"
              @click="handlePublish(row)"
            >
              发布
            </el-button>
            <el-button
              v-if="row.status === 'PUBLISHED'"
              type="warning"
              link
              size="small"
              @click="handleClose(row)"
            >
              下架
            </el-button>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request';
import { formatLoc } from '@/utils/location';

const loading = ref(false);
const list = ref([]);
const total = ref(0);
const deptList = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  status: '',
  deptId: '',
  sortField: '',
  sortDir: '',
});

const statusMap = { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已下架', EXPIRED: '已到期' };
const statusTagMap = { DRAFT: 'info', PUBLISHED: 'success', CLOSED: 'warning', EXPIRED: 'danger' };

function statusLabel(s) {
  return statusMap[s] || s;
}

function statusTagType(s) {
  return statusTagMap[s] || 'info';
}

async function fetchList() {
  loading.value = true;
  try {
    const res = await request.get('/jobs/list', { params: { ...query } });
    list.value = res.data?.rows || [];
    total.value = res.data?.total || 0;
  } catch {
    // 错误已在拦截器处理
  } finally {
    loading.value = false;
  }
}

async function fetchDeptList() {
  try {
    const res = await request.get('/depts/tree');
    // dept tree 可能嵌套，展平为一级列表
    function flatten(nodes) {
      const result = [];
      for (const n of nodes || []) {
        result.push(n);
        if (n.children) result.push(...flatten(n.children));
      }
      return result;
    }
    deptList.value = flatten(res.data || []);
  } catch {
    // ignore
  }
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.title = '';
  query.status = '';
  query.deptId = '';
  query.pageNum = 1;
  query.sortField = '';
  query.sortDir = '';
  fetchList();
}

/** 表格排序变化处理 */
function handleSortChange({ prop, order }) {
  if (order) {
    query.sortField = prop;
    query.sortDir = order === 'ascending' ? 'asc' : 'desc';
  } else {
    query.sortField = '';
    query.sortDir = '';
  }
  fetchList();
}

async function handlePublish(row) {
  try {
    await request.put(`/jobs/${row.jobId}/publish`);
    ElMessage.success('发布成功');
    fetchList();
  } catch { /* ignore */ }
}

async function handleClose(row) {
  try {
    await request.put(`/jobs/${row.jobId}/offline`);
    ElMessage.success('已下架');
    fetchList();
  } catch { /* ignore */ }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除岗位「${row.title}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await request.delete(`/jobs/${row.jobId}`);
    ElMessage.success('删除成功');
    fetchList();
  } catch {
    // 取消或错误
  }
}

onMounted(() => {
  fetchDeptList();
  fetchList();
});
</script>

<style scoped>
.job-list-page {
  max-width: 1200px;
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
