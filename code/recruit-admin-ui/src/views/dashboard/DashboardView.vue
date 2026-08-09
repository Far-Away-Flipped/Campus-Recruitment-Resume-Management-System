<template>
  <div class="dashboard-page">
    <!-- 欢迎区 -->
    <div class="welcome-bar">
      <div>
        <h2 class="welcome-title">你好，{{ userInfo?.realName || '管理员' }}</h2>
        <p class="welcome-sub">今天是 {{ todayStr }}，祝工作顺利。</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }" @click="$router.push('/recruit/jobs')">
          <div class="stat-inner">
            <div class="stat-icon" style="background: rgba(95,184,214,0.12); color: #5FB8D6;">
              <el-icon :size="28"><Briefcase /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ stats.jobCount }}</div>
              <div class="stat-label">在招岗位数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }" @click="$router.push('/recruit/resumes')">
          <div class="stat-inner">
            <div class="stat-icon" style="background: rgba(95,184,141,0.12); color: #5FB88D;">
              <el-icon :size="28"><DocumentAdd /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ stats.todayApplyCount }}</div>
              <div class="stat-label">今日投递数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }" @click="$router.push({path:'/recruit/resumes', query:{status:'PENDING'}})">
          <div class="stat-inner">
            <div class="stat-icon" style="background: rgba(232,163,61,0.12); color: #E8A33D;">
              <el-icon :size="28"><Clock /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ stats.pendingCount }}</div>
              <div class="stat-label">待筛选简历</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }" @click="$router.push('/recruit/resumes')">
          <div class="stat-inner">
            <div class="stat-icon" style="background: rgba(107,179,255,0.12); color: #6BB3FF;">
              <el-icon :size="28"><TrendCharts /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ stats.weekApplyCount }}</div>
              <div class="stat-label">本周新增</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口 -->
    <el-row :gutter="16" class="quick-row">
      <el-col :xs="24" :sm="12" class="quick-col">
        <el-card shadow="never" class="quick-card">
          <template #header><span class="card-header-title">快捷操作</span></template>
          <div class="quick-actions">
            <el-button type="primary" plain @click="$router.push('/recruit/jobs/create')">
              <el-icon><Plus /></el-icon> 发布岗位
            </el-button>
            <el-button type="success" plain @click="$router.push('/recruit/resumes')">
              <el-icon><Document /></el-icon> 简历筛选
            </el-button>
            <el-button type="warning" plain @click="$router.push('/recruit/reports')">
              <el-icon><DataAnalysis /></el-icon> 数据报表
            </el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" class="quick-col">
        <el-card shadow="never" class="quick-card">
          <template #header><span class="card-header-title">最近投递动态</span></template>
          <div v-if="recentList.length" class="recent-list">
            <div v-for="item in recentList" :key="item.id" class="recent-item" @click="$router.push('/recruit/resumes/' + item.id)">
              <span class="recent-name">{{ item.snapshotName }}</span>
              <span class="recent-job">投递了「{{ item.jobTitle }}」</span>
              <span class="recent-time">{{ item.applyTime }}</span>
            </div>
          </div>
          <el-empty v-else description="暂无投递记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import request from '@/utils/request';

const stats = reactive({
  jobCount: 0,
  todayApplyCount: 0,
  pendingCount: 0,
  weekApplyCount: 0,
});

const recentList = ref([]);
const userInfo = ref(null);

const todayStr = computedFormattedDate();

function computedFormattedDate() {
  const d = new Date();
  const weekNames = ['日', '一', '二', '三', '四', '五', '六'];
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${weekNames[d.getDay()]}`;
}

function formatDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

async function fetchStats() {
  try {
    // 获取岗位列表统计岗位数
    const jobRes = await request.get('/jobs/list', { params: { pageNum: 1, pageSize: 1 } });
    stats.jobCount = jobRes.data?.total ?? 0;

    // 获取本日投递趋势
    const today = formatDate(new Date());
    const todayRes = await request.get('/reports/apply-trend', {
      params: { startDate: today, endDate: today },
    });
    if (todayRes.data && todayRes.data.length > 0) {
      stats.todayApplyCount = todayRes.data.reduce((sum, d) => sum + (d.count || 0), 0);
    }

    // 获取本周投递趋势
    const now = new Date();
    const dayOfWeek = now.getDay() || 7;
    const monday = new Date(now);
    monday.setDate(now.getDate() - dayOfWeek + 1);
    const weekStart = formatDate(monday);
    const weekEnd = formatDate(now);
    const weekRes = await request.get('/reports/apply-trend', {
      params: { startDate: weekStart, endDate: weekEnd },
    });
    if (weekRes.data && weekRes.data.length > 0) {
      stats.weekApplyCount = weekRes.data.reduce((sum, d) => sum + (d.count || 0), 0);
    }

    // 获取待筛选简历数
    const pendingRes = await request.get('/resumes/list', {
      params: { pageNum: 1, pageSize: 1, status: 'PENDING' },
    });
    stats.pendingCount = pendingRes.data?.total ?? 0;

    // 最近投递
    const recentRes = await request.get('/resumes/list', {
      params: { pageNum: 1, pageSize: 5 },
    });
    recentList.value = (recentRes.data?.records || []).map(r => ({
      id: r.applicationId,
      snapshotName: r.snapshotName,
      jobTitle: r.jobTitle,
      applyTime: r.applyTime,
    }));
  } catch {
    // 数据加载失败时保持默认值
  }
}

onMounted(async () => {
  try {
    const res = await request.get('/auth/info');
    userInfo.value = res.data;
  } catch {
    // ignore
  }
  fetchStats();
});
</script>

<style scoped>
.dashboard-page {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-bar {
  margin-bottom: 20px;
}

.welcome-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.welcome-sub {
  font-size: 13px;
  color: #909399;
}

/* 统计卡片 */
.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-3px);
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

/* 快捷操作 */
.quick-row {
  margin-top: 0;
}

.quick-col {
  margin-bottom: 16px;
}

.quick-card {
  min-height: 180px;
}

.card-header-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.quick-actions .el-button {
  flex: 1;
  min-width: 100px;
}

/* 最近投递 */
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recent-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
  cursor: pointer;
}

.recent-item:last-child {
  border-bottom: none;
}

.recent-name {
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.recent-job {
  color: #606266;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-time {
  color: #c0c4cc;
  white-space: nowrap;
  font-size: 12px;
}

@media (max-width: 768px) {
  .stat-inner {
    gap: 10px;
  }
  .stat-icon {
    width: 44px;
    height: 44px;
    border-radius: 10px;
  }
  .stat-value {
    font-size: 22px;
  }
}
</style>
