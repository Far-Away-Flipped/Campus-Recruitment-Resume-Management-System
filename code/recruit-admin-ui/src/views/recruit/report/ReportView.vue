<template>
  <div class="report-page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="4.8">
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
      <el-col :xs="12" :sm="4.8">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }" @click="$router.push('/recruit/resumes')">
          <div class="stat-inner">
            <div class="stat-icon" style="background: rgba(95,184,141,0.12); color: #5FB88D;">
              <el-icon :size="28"><DocumentAdd /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ stats.totalApplyCount }}</div>
              <div class="stat-label">总投递数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="4.8">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }" @click="$router.push('/recruit/resumes?status=PENDING')">
          <div class="stat-inner">
            <div class="stat-icon" style="background: rgba(232,163,61,0.12); color: #E8A33D;">
              <el-icon :size="28"><Clock /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ stats.pendingCount }}</div>
              <div class="stat-label">待筛选简历数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="4.8">
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
      <el-col :xs="12" :sm="4.8">
        <el-card shadow="hover" class="stat-card" :body-style="{ padding: '20px' }">
          <div class="stat-inner">
            <div class="stat-icon" style="background: rgba(224,82,82,0.12); color: #E05252;">
              <el-icon :size="28"><DataLine /></el-icon>
            </div>
            <div class="stat-body">
              <div class="stat-value">{{ stats.passRate }}%</div>
              <div class="stat-label">筛选通过率</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日期范围筛选 -->
    <el-card shadow="never" class="chart-card">
      <div class="chart-header">
        <span class="chart-title">投递趋势</span>
        <el-date-picker
          v-model="trendDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          style="width: 260px;"
          @change="fetchTrend"
        />
      </div>
      <div class="chart-body" ref="trendChartRef" v-loading="trendLoading" style="height: 350px;"></div>
    </el-card>

    <!-- 四个分布图表 -->
    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-section-title">学校分布</span></template>
          <div class="chart-body" ref="schoolChartRef" v-loading="schoolLoading" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-section-title">学历分布</span></template>
          <div class="chart-body" ref="degreeChartRef" v-loading="degreeLoading" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-section-title">岗位投递排行</span></template>
          <div class="chart-body" ref="jobRankChartRef" v-loading="jobRankLoading" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12">
        <el-card shadow="never" class="chart-card">
          <template #header><span class="chart-section-title">渠道来源</span></template>
          <div class="chart-body" ref="channelChartRef" v-loading="channelLoading" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue';
import * as echarts from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart, BarChart, PieChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components';
import request from '@/utils/request';

echarts.use([CanvasRenderer, LineChart, BarChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent]);

// 统计数据
const stats = reactive({
  jobCount: 0,
  totalApplyCount: 0,
  pendingCount: 0,
  weekApplyCount: 0,
  passRate: 0,
});

// 日期范围（默认最近30天）
const trendDateRange = ref([]);
const trendLoading = ref(false);
const schoolLoading = ref(false);
const degreeLoading = ref(false);
const jobRankLoading = ref(false);
const channelLoading = ref(false);

// 图表 DOM 引用
const trendChartRef = ref(null);
const schoolChartRef = ref(null);
const degreeChartRef = ref(null);
const jobRankChartRef = ref(null);
const channelChartRef = ref(null);

// 图表实例
let trendChart = null;
let schoolChart = null;
let degreeChart = null;
let jobRankChart = null;
let channelChart = null;

function formatDate(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function initDefaultDateRange() {
  const end = new Date();
  const start = new Date();
  start.setDate(start.getDate() - 30);
  trendDateRange.value = [formatDate(start), formatDate(end)];
}

// 初始化图表
function initChart(ref, domRef) {
  return function () {
    const dom = domRef;
    if (!dom) return null;
    const instance = echarts.init(dom);
    return instance;
  }();
}

function resizeCharts() {
  trendChart?.resize();
  schoolChart?.resize();
  degreeChart?.resize();
  jobRankChart?.resize();
  channelChart?.resize();
}

// 获取统计数据
async function fetchStats() {
  try {
    // 在招岗位数
    const jobRes = await request.get('/jobs/list', { params: { pageNum: 1, pageSize: 1 } });
    stats.jobCount = jobRes.data?.total ?? 0;

    // 待筛选简历数
    const pendingRes = await request.get('/resumes/list', { params: { pageNum: 1, pageSize: 1, status: 'PENDING' } });
    stats.pendingCount = pendingRes.data?.total ?? 0;

    // 本周新增
    const now = new Date();
    const dayOfWeek = now.getDay() || 7;
    const monday = new Date(now);
    monday.setDate(now.getDate() - dayOfWeek + 1);
    const weekStart = formatDate(monday);
    const weekEnd = formatDate(now);
    const weekRes = await request.get('/reports/apply-trend', {
      params: { startDate: weekStart, endDate: weekEnd },
    });
    if (weekRes.data?.length) {
      stats.weekApplyCount = weekRes.data.reduce((sum, d) => sum + (d.count || 0), 0);
    }

    // 总投递数（30天内）
    const end30 = new Date();
    const start30 = new Date();
    start30.setDate(start30.getDate() - 30);
    const trendRes = await request.get('/reports/apply-trend', {
      params: { startDate: formatDate(start30), endDate: formatDate(end30) },
    });
    if (trendRes.data?.length) {
      stats.totalApplyCount = trendRes.data.reduce((sum, d) => sum + (d.count || 0), 0);
    }

    // 筛选通过率
    const passedRes = await request.get('/resumes/list', { params: { pageNum: 1, pageSize: 1, status: 'PASSED' } });
    const totalRes = await request.get('/resumes/list', { params: { pageNum: 1, pageSize: 1 } });
    const passedTotal = passedRes.data?.total ?? 0;
    const allTotal = totalRes.data?.total ?? 0;
    stats.passRate = allTotal > 0 ? Math.round((passedTotal / allTotal) * 100) : 0;
  } catch {
    // 错误已在拦截器处理
  }
}

// 获取投递趋势
async function fetchTrend() {
  trendLoading.value = true;
  try {
    const params = {};
    if (trendDateRange.value?.length === 2) {
      params.startDate = trendDateRange.value[0];
      params.endDate = trendDateRange.value[1];
    }
    const res = await request.get('/reports/apply-trend', { params });
    const data = res.data || [];
    const xData = data.map(d => d.date || '');
    const yData = data.map(d => d.count || 0);
    trendChart?.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: xData, boundaryGap: false },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{
        name: '投递数',
        type: 'line',
        data: yData,
        smooth: true,
        lineStyle: { color: '#5FB8D6', width: 3 },
        itemStyle: { color: '#5FB8D6' },
        areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(95,184,214,0.3)' },
          { offset: 1, color: 'rgba(95,184,214,0.02)' },
        ]) },
      }],
    }, true);
  } catch { /* ignore */ } finally {
    trendLoading.value = false;
  }
}

// 学校分布
async function fetchSchoolDistribution() {
  schoolLoading.value = true;
  try {
    const res = await request.get('/reports/school-distribution');
    const data = res.data || [];
    schoolChart?.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: data.map(d => d.name || ''), inverse: true },
      series: [{
        name: '投递数',
        type: 'bar',
        data: data.map(d => d.value || 0),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#5FB8D6' },
            { offset: 1, color: '#6BB3FF' },
          ]),
          borderRadius: [0, 4, 4, 0],
        },
      }],
    }, true);
  } catch { /* ignore */ } finally {
    schoolLoading.value = false;
  }
}

// 学历分布
async function fetchDegreeDistribution() {
  degreeLoading.value = true;
  try {
    const res = await request.get('/reports/degree-distribution');
    const data = res.data || [];
    degreeChart?.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        name: '学历分布',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        label: { show: true, formatter: '{b}\n{d}%' },
        data: data.map(d => ({ name: d.name || '', value: d.value || 0 })),
        color: ['#5FB8D6', '#5FB88D', '#E8A33D', '#E05252', '#6BB3FF', '#C0C4CC'],
      }],
    }, true);
  } catch { /* ignore */ } finally {
    degreeLoading.value = false;
  }
}

// 岗位排行
async function fetchJobRanking() {
  jobRankLoading.value = true;
  try {
    const res = await request.get('/reports/job-ranking');
    const data = res.data || [];
    jobRankChart?.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: data.map(d => d.name || ''), inverse: true,
        axisLabel: { formatter: v => v.length > 10 ? v.slice(0, 10) + '...' : v } },
      series: [{
        name: '投递数',
        type: 'bar',
        data: data.map(d => d.value || 0),
        itemStyle: { color: '#E8A33D', borderRadius: [0, 4, 4, 0] },
      }],
    }, true);
  } catch { /* ignore */ } finally {
    jobRankLoading.value = false;
  }
}

// 渠道来源
async function fetchChannelSource() {
  channelLoading.value = true;
  try {
    const res = await request.get('/reports/source-distribution');
    const data = res.data || [];
    channelChart?.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        name: '渠道来源',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '45%'],
        label: { show: true, formatter: '{b}\n{d}%' },
        data: data.map(d => ({ name: d.name || '', value: d.value || 0 })),
        color: ['#5FB8D6', '#5FB88D', '#E8A33D', '#6BB3FF', '#E05252', '#C0C4CC'],
      }],
    }, true);
  } catch { /* ignore */ } finally {
    channelLoading.value = false;
  }
}

onMounted(async () => {
  initDefaultDateRange();
  await nextTick();

  // 初始化各个图表实例
  trendChart = echarts.init(trendChartRef.value);
  schoolChart = echarts.init(schoolChartRef.value);
  degreeChart = echarts.init(degreeChartRef.value);
  jobRankChart = echarts.init(jobRankChartRef.value);
  channelChart = echarts.init(channelChartRef.value);

  // 并行加载所有数据
  await Promise.all([
    fetchStats(),
    fetchTrend(),
    fetchSchoolDistribution(),
    fetchDegreeDistribution(),
    fetchJobRanking(),
    fetchChannelSource(),
  ]);

  window.addEventListener('resize', resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts);
  trendChart?.dispose();
  schoolChart?.dispose();
  degreeChart?.dispose();
  jobRankChart?.dispose();
  channelChart?.dispose();
});
</script>

<style scoped>
.report-page {
  max-width: 1400px;
  margin: 0 auto;
}

/* 统计卡片 */
.stat-row {
  margin-bottom: 16px;
}

.stat-row .el-col {
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
  gap: 14px;
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

/* 图表区 */
.chart-card {
  margin-bottom: 16px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 16px 16px 0;
}

.chart-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.chart-section-title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.chart-body {
  width: 100%;
}

.chart-row {
  margin-top: 0;
}

@media (max-width: 768px) {
  .stat-inner {
    gap: 8px;
  }
  .stat-icon {
    width: 40px;
    height: 40px;
    border-radius: 8px;
  }
  .stat-value {
    font-size: 20px;
  }
}
</style>
