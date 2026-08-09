<template>
  <div class="audit-page">
    <div class="page-header">
      <h2>操作审计日志</h2>
      <p class="page-desc">根据《个人信息保护法》要求，记录所有对个人简历数据的访问操作</p>
    </div>

    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="操作类型">
          <el-select v-model="query.operationType" placeholder="全部" clearable style="width:180px">
            <el-option v-for="t in opTypes" :key="t" :label="formatOpType(t)" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="query.operatorName" placeholder="搜索操作人" clearable style="width:180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card style="margin-top:16px">
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="createTime" label="操作时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="operationType" label="操作类型" width="140">
          <template #default="{ row }">
            <el-tag :type="tagType(row.operationType)" size="small">{{ formatOpType(row.operationType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetStudentName" label="目标学生" width="120" />
        <el-table-column prop="operationDetail" label="操作详情" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import request from '@/utils/request';

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const opTypes = ref([]);

const query = reactive({
  pageNum: 1,
  pageSize: 20,
  operationType: '',
  operatorName: ''
});

const opTypeLabels = {
  VIEW_RESUME: '查看简历',
  DOWNLOAD_ATTACHMENT: '下载附件',
  EXPORT_RESUME: '导出简历',
  SCREEN_PASS: '筛选通过',
  SCREEN_ELIMINATE: '筛选淘汰',
  BATCH_SCREEN: '批量筛选',
  ADD_REMARK: '添加备注',
  STATUS_CHANGE: '状态变更',
  VIEW_AUDIT_LOG: '查看审计日志'
};

function formatOpType(type) {
  return opTypeLabels[type] || type || '—';
}

function tagType(type) {
  const map = {
    VIEW_RESUME: '', DOWNLOAD_ATTACHMENT: 'warning', EXPORT_RESUME: 'danger',
    SCREEN_PASS: 'success', SCREEN_ELIMINATE: 'danger', BATCH_SCREEN: 'info',
    ADD_REMARK: '', STATUS_CHANGE: 'info', VIEW_AUDIT_LOG: ''
  };
  return map[type] || 'info';
}

function formatTime(t) {
  if (!t) return '';
  return t.replace('T', ' ');
}

async function loadData() {
  loading.value = true;
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize };
    if (query.operationType) params.operationType = query.operationType;
    if (query.operatorName) params.operatorName = query.operatorName;

    const res = await request.get('/audit/list', { params });
    if (res.data) {
      tableData.value = res.data.rows || [];
      total.value = res.data.total || 0;
    }
  } catch {
    tableData.value = [];
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.operationType = '';
  query.operatorName = '';
  query.pageNum = 1;
  loadData();
}

onMounted(async () => {
  try {
    const res = await request.get('/audit/types');
    if (res.data) opTypes.value = res.data;
  } catch { /* ignore */ }
  loadData();
});
</script>

<style scoped>
.audit-page { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0 0 4px; font-size: 20px; }
.page-desc { color: #909399; font-size: 13px; margin: 0; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
