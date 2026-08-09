<template>
  <div class="student-list-page">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-bar-card">
      <el-form :inline="true" :model="query" size="default">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="手机号/姓名" clearable style="width: 220px;" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px;">
            <el-option label="正常" value="ACTIVE" />
            <el-option label="已禁用" value="DISABLED" />
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

    <!-- 表格 -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="list"
        stripe
        size="small"
        style="width: 100%;"
        @sort-change="handleSortChange"
        @row-click="handleDetail"
      >
        <el-table-column prop="studentId" label="ID" width="80" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="schoolName" label="毕业院校" min-width="140" sortable="custom" show-overflow-tooltip />
        <el-table-column prop="major" label="专业" min-width="120" sortable="custom" show-overflow-tooltip />
        <el-table-column prop="createTime" label="注册时间" width="170" sortable="custom" />
        <el-table-column label="账号状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
              {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyCount" label="投递数" width="80" align="center" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click.stop="handleDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              type="warning"
              link
              size="small"
              @click.stop="handleToggleStatus(row)"
            >
              禁用
            </el-button>
            <el-button
              v-else
              type="success"
              link
              size="small"
              @click.stop="handleToggleStatus(row)"
            >
              启用
            </el-button>
            <el-button type="danger" link size="small" @click.stop="handleDelete(row)">删除</el-button>
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

const router = useRouter();
const loading = ref(false);
const list = ref([]);
const total = ref(0);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  sortField: 'createTime',
  sortOrder: 'desc',
});

/** 获取学生用户列表 */
async function fetchList() {
  loading.value = true;
  try {
    const res = await request.get('/students/list', { params: { ...query } });
    list.value = res.data?.rows || [];
    total.value = res.data?.total || 0;
  } catch {
    // ignore
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  fetchList();
}

function handleReset() {
  query.keyword = '';
  query.status = '';
  query.pageNum = 1;
  query.sortField = 'createTime';
  query.sortOrder = 'desc';
  fetchList();
}

/** 排序变更 */
function handleSortChange({ prop, order }) {
  if (!order) {
    query.sortField = 'createTime';
    query.sortOrder = 'desc';
  } else {
    query.sortField = prop;
    query.sortOrder = order === 'ascending' ? 'asc' : 'desc';
  }
  query.pageNum = 1;
  fetchList();
}

/** 跳转详情页 */
function handleDetail(row) {
  router.push({ name: 'student-detail', params: { id: row.studentId } });
}

/** 启用/禁用切换 */
async function handleToggleStatus(row) {
  const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const action = newStatus === 'ACTIVE' ? '启用' : '禁用';
  try {
    await ElMessageBox.confirm(`确认${action}学生「${row.phone}」吗？`, '确认操作', {
      confirmButtonText: `确认${action}`,
      cancelButtonText: '取消',
      type: 'warning',
    });
    await request.put(`/students/${row.studentId}/status`, null, { params: { status: newStatus } });
    ElMessage.success(`${action}成功`);
    fetchList();
  } catch {
    // 取消或错误
  }
}

/** 逻辑删除学生账号 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除学生「${row.phone}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await request.delete(`/students/${row.studentId}`);
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
.student-list-page {
  max-width: 1400px;
  margin: 0 auto;
}

.search-bar-card {
  margin-bottom: 12px;
}

.search-bar-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
