<template>
  <div class="job-detail-page">
    <div class="container">
      <!-- ====== 加载态：骨架屏 ====== -->
      <template v-if="loading">
        <div class="detail-layout">
          <div class="detail-main">
            <div class="skeleton skeleton--title"></div>
            <div class="skeleton skeleton--meta"></div>
            <div class="skeleton skeleton--text-block"></div>
            <div class="skeleton skeleton--text-block"></div>
            <div class="skeleton skeleton--text-block skeleton--text-block--short"></div>
          </div>
          <div class="detail-sidebar">
            <div class="skeleton skeleton--card"></div>
          </div>
        </div>
      </template>

      <!-- ====== 404 态 ====== -->
      <template v-else-if="!job">
        <div class="not-found">
          <div class="not-found__icon">
            <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" opacity="0.3">
              <circle cx="12" cy="12" r="10"/><path d="M16 16s-1.5-2-4-2-4 2-4 2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/>
            </svg>
          </div>
          <h2 class="not-found__title">岗位不存在</h2>
          <p class="not-found__desc">该岗位可能已下架或链接地址有误</p>
          <router-link to="/jobs" class="not-found__link">返回岗位列表</router-link>
        </div>
      </template>

      <!-- ====== 正常态 ====== -->
      <template v-else>
        <!-- 面包屑 -->
        <nav class="breadcrumb">
          <router-link to="/jobs" class="breadcrumb__link">在招岗位</router-link>
          <span class="breadcrumb__sep">/</span>
          <span class="breadcrumb__current">{{ job.title }}</span>
        </nav>

        <div class="detail-layout" v-motion-fade="{ y: 24 }">
          <!-- 左侧：岗位详情 -->
          <div class="detail-main">
            <!-- 标题区 -->
            <div class="detail-header">
              <h1 class="detail-title">{{ job.title }}</h1>
              <div class="detail-status" v-if="job.status === 'CLOSED' || isDeadlinePassed">
                <span class="detail-status__badge detail-status__badge--closed">已截止</span>
              </div>
            </div>

            <!-- Meta 信息 -->
            <div class="detail-meta">
              <div class="detail-meta__item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                <span class="detail-meta__label">部门</span>
                <span class="detail-meta__value">{{ job.deptName || '--' }}</span>
              </div>
              <div class="detail-meta__item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="10" r="3"/><path d="M12 21.7C17.3 17 20 13 20 10a8 8 0 1 0-16 0c0 3 2.7 7 8 11.7z"/></svg>
                <span class="detail-meta__label">工作地点</span>
                <span class="detail-meta__value">{{ formatLoc(job.location) || '北京' }}</span>
              </div>
              <div class="detail-meta__item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/><path d="M6 12v5c0 2 2 3 6 3s6-1 6-3v-5"/></svg>
                <span class="detail-meta__label">学历要求</span>
                <span class="detail-meta__value">{{ formatDegree(job.degreeRequirement) || '本科及以上' }}</span>
              </div>
              <div v-if="job.categoryName" class="detail-meta__item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
                <span class="detail-meta__label">岗位类别</span>
                <span class="detail-meta__value">{{ job.categoryName }}</span>
              </div>
              <div v-if="job.deadline" class="detail-meta__item">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                <span class="detail-meta__label">截止日期</span>
                <span class="detail-meta__value" :class="{ 'text-danger': isDeadlinePassed }">{{ formatDate(job.deadline) }}</span>
              </div>
            </div>

            <!-- 标签 -->
            <div class="detail-tags" v-if="jobTags.length">
              <span v-for="tag in jobTags" :key="tag" class="detail-tag">{{ tag }}</span>
            </div>

            <!-- 分割线 -->
            <div class="detail-divider"></div>

            <!-- 岗位职责 -->
            <section class="detail-section" v-if="job.description">
              <h2 class="detail-section__title">
                <span class="detail-section__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                </span>
                岗位职责
              </h2>
              <div class="detail-section__content" v-html="job.description"></div>
            </section>

            <!-- 任职要求 -->
            <section class="detail-section" v-if="job.requirement">
              <h2 class="detail-section__title">
                <span class="detail-section__icon">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/></svg>
                </span>
                任职要求
              </h2>
              <div class="detail-section__content" v-html="job.requirement"></div>
            </section>
          </div>

          <!-- 右侧：悬浮投递卡片 -->
          <aside class="detail-sidebar">
            <div class="apply-card" :class="{ 'apply-card--sticky': isDesktop }">
              <!-- 公司 Logo -->
              <div class="apply-card__logo">
                <img src="/logo.png" alt="遨天科技" class="apply-card__logo-img" />
                <span class="apply-card__logo-text">遨天科技</span>
              </div>

              <!-- 截止日期倒计时 -->
              <div class="apply-card__countdown" v-if="job.deadline && !isDeadlinePassed">
                <span class="apply-card__countdown-label">距离截止还有</span>
                <div class="apply-card__countdown-timer">
                  <span class="apply-card__countdown-num">{{ countdown.days }}</span>
                  <span class="apply-card__countdown-unit">天</span>
                  <span class="apply-card__countdown-num">{{ countdown.hours }}</span>
                  <span class="apply-card__countdown-unit">时</span>
                  <span class="apply-card__countdown-num">{{ countdown.minutes }}</span>
                  <span class="apply-card__countdown-unit">分</span>
                </div>
              </div>
              <div class="apply-card__countdown apply-card__countdown--closed" v-else-if="isDeadlinePassed">
                <span class="apply-card__countdown-label">该岗位投递已截止</span>
              </div>

              <!-- 投递按钮 -->
              <button
                v-if="job.hasApplied"
                class="apply-card__btn apply-card__btn--applied"
                disabled
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                已投递
              </button>
              <button
                v-else-if="isDeadlinePassed || job.status === 'CLOSED'"
                class="apply-card__btn apply-card__btn--closed"
                disabled
              >已截止</button>
              <button
                v-else-if="!isLoggedIn"
                class="apply-card__btn apply-card__btn--primary"
                @click="goToLogin"
              >立即投递</button>
              <router-link
                v-else
                :to="`/apply/${job.jobId}`"
                class="apply-card__btn apply-card__btn--primary"
              >立即投递</router-link>

              <!-- 附加信息 -->
              <div class="apply-card__info">
                <div class="apply-card__info-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  <span>发布于 {{ formatDate(job.createTime) || '--' }}</span>
                </div>
                <div class="apply-card__info-item" v-if="job.location">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="10" r="3"/><path d="M12 21.7C17.3 17 20 13 20 10a8 8 0 1 0-16 0c0 3 2.7 7 8 11.7z"/></svg>
                  <span>{{ formatLoc(job.location) }}</span>
                </div>
              </div>
            </div>
          </aside>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/utils/axios';
import { formatLoc, formatDegree, parseTags } from '@/utils/location';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const isLoggedIn = computed(() => auth.isLoggedIn);

// ---- 数据状态 ----
const job = ref(null);

/** 解析岗位标签为数组（tags 存 JSON 数组文本，兼容旧坏数据） */
const jobTags = computed(() => parseTags(job.value?.tags));
const loading = ref(true);

// ---- 桌面端判断（用于 sticky） ----
const isDesktop = ref(window.innerWidth >= 1024);

function onResize() {
  isDesktop.value = window.innerWidth >= 1024;
}

// ---- 截止日期倒计时 ----
const countdown = ref({ days: 0, hours: 0, minutes: 0 });
let countdownTimer = null;

// 截止是否已过：由后端实时返回的 status 判断（EXPIRED/CLOSED），不使用前端本地时间
const isDeadlinePassed = computed(() => {
  const s = job.value?.status;
  return s === 'EXPIRED' || s === 'CLOSED';
});

function updateCountdown() {
  if (!job.value?.deadline) return;
  const deadline = new Date(job.value.deadline).getTime();
  const now = Date.now();
  const diff = deadline - now;

  if (diff <= 0) {
    countdown.value = { days: 0, hours: 0, minutes: 0 };
    if (countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
    return;
  }

  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));

  countdown.value = { days, hours, minutes };
}

// ---- 跳转登录 ----
function goToLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } });
}

// ---- 工具函数 ----
function formatDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// ---- 获取岗位详情 ----
async function fetchJobDetail() {
  loading.value = true;
  try {
    const jobId = route.params.id;
    const res = await api.get(`/jobs/${jobId}`);
    if (res.code === 200) {
      job.value = res.data;
      // 启动倒计时（详情接口已保证 deadline 未过期；有 deadline 即启动）
      updateCountdown();
      if (res.data?.deadline) {
        countdownTimer = setInterval(updateCountdown, 60000); // 每分钟更新
      }
    }
  } catch {
    // 岗位不存在/接口错误：拦截器已 toast，这里降级为 404 空态
    job.value = null;
  } finally {
    loading.value = false;
  }
}

// ---- 生命周期 ----
onMounted(() => {
  window.addEventListener('resize', onResize);
  fetchJobDetail();
});

onUnmounted(() => {
  window.removeEventListener('resize', onResize);
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
});
</script>

<style scoped>
.container { max-width: 1200px; margin: 0 auto; padding: 0 24px; }

/* ====== 面包屑 ====== */
.breadcrumb {
  padding: 24px 0;
  font-size: 13px;
  color: #6E7D8A;
}
.breadcrumb__link {
  color: #5FB8D6;
  text-decoration: none;
  transition: color 0.2s;
}
.breadcrumb__link:hover {
  color: #6BB3FF;
}
.breadcrumb__sep {
  margin: 0 8px;
  color: #6E7D8A;
}
.breadcrumb__current {
  color: #9CA3AF;
  text-shadow: 0 0 8px var(--glow-color);
}

/* ====== 双栏布局 ====== */
.detail-layout {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 48px;
  padding-bottom: 80px;
  align-items: start;
}

/* ====== 左侧主体 ====== */
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.detail-title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 1px;
  line-height: 1.3;
  text-shadow: 0 0 12px var(--glow-strong), 0 0 20px var(--glow-color);
}
.detail-status__badge {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 4px;
}
.detail-status__badge--closed {
  background: rgba(224, 82, 82, 0.12);
  color: #E05252;
}

/* Meta 信息网格 */
.detail-meta {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}
.detail-meta__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--bg-glass);
  backdrop-filter: blur(var(--glass-blur));
  border-radius: 8px;
  border: 1px solid rgba(95, 184, 214, 0.1);
}
.detail-meta__item svg {
  color: #5FB8D6;
  flex-shrink: 0;
}
.detail-meta__label {
  font-size: 12px;
  color: #6E7D8A;
  white-space: nowrap;
}
.detail-meta__value {
  font-size: 14px;
  color: #ccc;
  font-weight: 500;
}
.text-danger {
  color: #E05252 !important;
}

/* 标签 */
.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.detail-tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 4px;
  background: rgba(95, 184, 214, 0.1);
  color: #5FB8D6;
}

/* 分割线 */
.detail-divider {
  height: 1px;
  background: rgba(95, 184, 214, 0.1);
  margin: 24px 0;
}

/* 内容区块 */
.detail-section {
  margin-bottom: 36px;
}
.detail-section__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid rgba(95, 184, 214, 0.2);
}
.detail-section__icon {
  display: flex;
  align-items: center;
  color: #5FB8D6;
}

/* 富文本内容区（v-html 渲染后端富文本） */
.detail-section__content {
  color: #9CA3AF;
  font-size: 15px;
  line-height: 1.9;
}
/* 富文本内元素样式重置 */
.detail-section__content :deep(h1),
.detail-section__content :deep(h2),
.detail-section__content :deep(h3),
.detail-section__content :deep(h4) {
  color: #fff;
  margin: 20px 0 12px;
  font-weight: 600;
}
.detail-section__content :deep(h3) { font-size: 17px; }
.detail-section__content :deep(h4) { font-size: 15px; }
.detail-section__content :deep(p) {
  margin-bottom: 12px;
}
.detail-section__content :deep(ul),
.detail-section__content :deep(ol) {
  padding-left: 20px;
  margin-bottom: 12px;
}
.detail-section__content :deep(li) {
  margin-bottom: 6px;
}
.detail-section__content :deep(strong) {
  color: #ddd;
  font-weight: 600;
}
.detail-section__content :deep(a) {
  color: #5FB8D6;
  text-decoration: none;
}
.detail-section__content :deep(a:hover) {
  text-decoration: underline;
}
.detail-section__content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 12px 0;
}
.detail-section__content :deep(th),
.detail-section__content :deep(td) {
  padding: 8px 12px;
  border: 1px solid rgba(95, 184, 214, 0.15);
  text-align: left;
  font-size: 14px;
}
.detail-section__content :deep(th) {
  background: rgba(95, 184, 214, 0.08);
  color: #ccc;
  font-weight: 600;
}
.detail-section__content :deep(img) {
  max-width: 100%;
  border-radius: 6px;
}

/* ====== 右侧投递卡片 ====== */
.detail-sidebar {
  position: relative;
}
.apply-card {
  background: var(--bg-glass-strong);
  backdrop-filter: blur(var(--glass-blur-heavy)) saturate(150%);
  border: 1px solid rgba(95, 184, 214, 0.15);
  border-radius: 12px;
  padding: 32px 24px;
  text-align: center;
  box-shadow: var(--shadow-glow-md);
}
.apply-card--sticky {
  position: sticky;
  top: 88px;
}

/* 公司 Logo */
.apply-card__logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 24px;
}
.apply-card__logo-img {
  height: 36px;
}
.apply-card__logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2px;
}

/* 倒计时 */
.apply-card__countdown {
  margin-bottom: 24px;
  padding: 16px;
  background: var(--bg-trans);
  backdrop-filter: blur(6px);
  border-radius: 8px;
  border: 1px solid rgba(95, 184, 214, 0.1);
}
.apply-card__countdown--closed {
  border-color: rgba(224, 82, 82, 0.15);
  background: rgba(224, 82, 82, 0.04);
}
.apply-card__countdown-label {
  display: block;
  font-size: 12px;
  color: #6E7D8A;
  margin-bottom: 8px;
  letter-spacing: 1px;
}
.apply-card__countdown-timer {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 2px;
}
.apply-card__countdown-num {
  font-family: 'Share Tech Mono', 'Courier New', monospace;
  font-size: 22px;
  font-weight: 700;
  color: #6BB3FF;
  min-width: 32px;
  text-align: center;
}
.apply-card__countdown-unit {
  font-size: 12px;
  color: #6E7D8A;
  margin-right: 6px;
}
.apply-card__countdown--closed .apply-card__countdown-label {
  color: #E05252;
  margin-bottom: 0;
  font-size: 13px;
}

/* 投递按钮 */
.apply-card__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 14px 0;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 700;
  font-family: inherit;
  text-decoration: none;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
  letter-spacing: 2px;
}
.apply-card__btn--primary {
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  color: #0A0E17;
}
.apply-card__btn--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(95, 184, 214, 0.3);
}
.apply-card__btn--applied {
  background: rgba(95, 184, 214, 0.1);
  color: #5FB8D6;
  cursor: not-allowed;
}
.apply-card__btn--closed {
  background: rgba(224, 82, 82, 0.08);
  color: #E05252;
  cursor: not-allowed;
}

/* 附加信息 */
.apply-card__info {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(95, 184, 214, 0.08);
}
.apply-card__info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6E7D8A;
  margin-bottom: 8px;
}
.apply-card__info-item svg {
  flex-shrink: 0;
}
.apply-card__info-item:last-child {
  margin-bottom: 0;
}

/* ====== 骨架屏 ====== */
.skeleton {
  background: linear-gradient(90deg, #152535 25%, rgba(95, 184, 214, 0.06) 50%, #152535 75%);
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.5s ease-in-out infinite;
  border-radius: 8px;
}
@keyframes skeleton-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
.skeleton--title {
  height: 36px;
  width: 60%;
  margin-bottom: 24px;
}
.skeleton--meta {
  height: 52px;
  width: 100%;
  margin-bottom: 20px;
}
.skeleton--text-block {
  height: 16px;
  width: 100%;
  margin-bottom: 12px;
}
.skeleton--text-block--short {
  width: 70%;
}
.skeleton--card {
  height: 280px;
  width: 100%;
}

/* ====== 404 态 ====== */
.not-found {
  text-align: center;
  padding: 100px 0 120px;
}
.not-found__icon {
  margin-bottom: 20px;
  color: #6E7D8A;
}
.not-found__title {
  font-size: 22px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 8px;
}
.not-found__desc {
  font-size: 14px;
  color: #9CA3AF;
  margin-bottom: 32px;
}
.not-found__link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 32px;
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  color: #0A0E17;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  letter-spacing: 1px;
  transition: transform 0.2s, box-shadow 0.2s;
}
.not-found__link:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 24px rgba(95, 184, 214, 0.3);
}

/* ====== 响应式 ====== */
@media (max-width: 1024px) {
  .detail-layout {
    grid-template-columns: 1fr;
    gap: 32px;
  }
  .detail-sidebar {
    order: -1; /* 移动端投递卡片放上方 */
  }
  .apply-card {
    position: static;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
    text-align: left;
    padding: 20px;
  }
  .apply-card__logo {
    margin-bottom: 0;
  }
  .apply-card__countdown {
    margin-bottom: 0;
    flex: 1;
    text-align: center;
  }
  .apply-card__btn {
    width: auto;
    padding: 10px 28px;
    flex-shrink: 0;
  }
  .apply-card__info {
    display: none;
  }
}

@media (max-width: 640px) {
  .detail-title {
    font-size: 22px;
  }
  .detail-meta {
    grid-template-columns: 1fr;
  }
  .detail-sidebar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 90;
    background: rgba(10, 14, 23, 0.95);
    backdrop-filter: blur(10px);
    border-top: 1px solid var(--color-border);
    padding: 12px 16px;
    padding-bottom: calc(12px + env(safe-area-inset-bottom, 16px));
  }
  .apply-card {
    flex-direction: row;
    padding: 0;
    background: none;
    border: none;
    justify-content: space-between;
    align-items: center;
  }
  .apply-card__logo,
  .apply-card__countdown,
  .apply-card__info {
    display: none;
  }
  .apply-card__btn {
    flex: 1;
    min-height: var(--touch-min);
  }
  .detail-layout {
    padding-bottom: 80px;
  }
  .detail-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
