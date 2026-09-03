<template>
  <div class="applications-page" v-motion-fade="{ y: 24 }">
    <!-- 页头 -->
    <div class="page-header">
      <h1 class="page-title">我的投递</h1>
      <p class="page-subtitle">查看所有已投递岗位的处理进度</p>
    </div>

    <!-- 加载态 -->
    <LoadingSpinner :visible="loading" text="加载投递记录..." />

    <!-- 错误信息 -->
    <div class="message message--error" v-if="errorMsg">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
      {{ errorMsg }}
    </div>

    <!-- 列表 -->
    <template v-if="!loading && !errorMsg">
      <div class="app-list" v-if="applications.length > 0">
        <div class="list-header">
          <span class="list-header__count">共 {{ total }} 条记录</span>
        </div>
        <div
          class="app-card"
          v-for="app in applications"
          :key="app.applicationId"
          @click="openDetail(app)"
        >
          <div class="app-card__main">
            <h3 class="app-card__title">{{ app.jobTitle }}</h3>
            <div class="app-card__meta">
              <span class="app-card__dept">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="7" width="18" height="13" rx="2"/></svg>
                {{ app.company || '--' }}
              </span>
              <span class="app-card__time">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                {{ formatTime(app.applyTime) }}
              </span>
            </div>
          </div>
          <div class="app-card__status">
            <span class="status-tag" :class="statusClass(app.status)">
              <span class="status-tag__dot"></span>
              {{ app.statusLabel || statusLabel(app.status) }}
            </span>
            <svg class="app-card__chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination" v-if="total > pageSize">
          <button
            class="page-btn"
            :disabled="pageNum <= 1"
            @click="goPage(pageNum - 1)"
          >上一页</button>
          <span class="page-info">{{ pageNum }} / {{ totalPages }}</span>
          <button
            class="page-btn"
            :disabled="pageNum >= totalPages"
            @click="goPage(pageNum + 1)"
          >下一页</button>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-state" v-else>
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="12" y1="12" x2="12" y2="18"/>
          <line x1="9" y1="15" x2="15" y2="15"/>
        </svg>
        <p>暂无投递记录</p>
        <span>快去浏览心仪的岗位吧</span>
        <router-link to="/jobs" class="empty-btn">浏览岗位</router-link>
      </div>
    </template>

    <!-- 详情弹窗 -->
    <Transition name="modal">
      <div class="modal-overlay" v-if="detailVisible" @click.self="closeDetail">
        <div class="modal-card">
          <div class="modal-card__header">
            <h3>投递详情</h3>
            <button class="modal-close" @click="closeDetail">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <LoadingSpinner :visible="detailLoading" text="加载详情..." />
          <template v-if="!detailLoading && detail">
            <!-- 基本信息 -->
            <div class="detail-section">
              <h4 class="detail-section__title">基本信息</h4>
              <div class="detail-grid">
                <div class="detail-item">
                  <span class="detail-item__label">投递岗位</span>
                  <span class="detail-item__value">{{ detail.jobTitle || '--' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-item__label">所属部门</span>
                  <span class="detail-item__value">{{ detail.company || '--' }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-item__label">投递时间</span>
                  <span class="detail-item__value">{{ formatTime(detail.applyTime) }}</span>
                </div>
                <div class="detail-item">
                  <span class="detail-item__label">当前状态</span>
                  <span class="detail-item__value">
                    <span class="status-tag" :class="statusClass(detail.status)">
                      <span class="status-tag__dot"></span>
                      {{ detail.statusLabel || statusLabel(detail.status) }}
                    </span>
                  </span>
                </div>
                <div class="detail-item" v-if="detail.sourceLabel">
                  <span class="detail-item__label">渠道来源</span>
                  <span class="detail-item__value">{{ detail.sourceLabel }}</span>
                </div>
              </div>
            </div>
            <!-- 投递简历（快照：教育/实习/技能/社团） -->
            <div class="detail-section resume-section" v-if="hasResumeContent">
              <h4 class="detail-section__title">投递简历</h4>
              <!-- 教育经历 -->
              <div class="resume-block" v-if="detailEducations.length > 0">
                <div class="resume-block__label">教育经历</div>
                <div class="resume-block__item" v-for="(edu, i) in detailEducations" :key="'edu' + i">
                  <div class="resume-block__line">
                    <span class="resume-block__main">{{ edu.schoolName || '--' }}</span>
                    <span v-if="edu.major" class="resume-block__tag">{{ edu.major }}</span>
                    <span v-if="edu.degree" class="resume-block__tag">{{ formatDegree(edu.degree) }}</span>
                  </div>
                  <div class="resume-block__sub" v-if="edu.startDate || edu.endDate">
                    {{ edu.startDate || '?' }} ~ {{ edu.endDate || '至今' }}
                  </div>
                  <div class="resume-block__desc" v-if="edu.gpa">GPA：{{ edu.gpa }}</div>
                </div>
              </div>
              <!-- 实习/项目经历 -->
              <div class="resume-block" v-if="detailInternships.length > 0">
                <div class="resume-block__label">实习/项目经历</div>
                <div class="resume-block__item" v-for="(it, i) in detailInternships" :key="'it' + i">
                  <div class="resume-block__line">
                    <span class="resume-block__main">{{ it.orgName || '--' }}</span>
                    <span class="resume-block__tag">{{ internTypeLabel(it.recordType) }}</span>
                    <span v-if="it.position" class="resume-block__tag">{{ it.position }}</span>
                  </div>
                  <div class="resume-block__sub" v-if="it.startDate || it.endDate">
                    {{ it.startDate || '?' }} ~ {{ it.endDate || '至今' }}
                  </div>
                  <div class="resume-block__desc" v-if="it.description">{{ it.description }}</div>
                </div>
              </div>
              <!-- 技能/证书/语言能力 -->
              <div class="resume-block" v-if="detailCertificates.length > 0">
                <div class="resume-block__label">技能证书/语言能力</div>
                <div class="resume-block__item" v-for="(ct, i) in detailCertificates" :key="'ct' + i">
                  <div class="resume-block__line">
                    <span class="resume-block__main">{{ ct.certName || '--' }}</span>
                    <span class="resume-block__tag">{{ certTypeLabel(ct.certType) }}</span>
                    <span v-if="ct.certLevel" class="resume-block__tag">{{ ct.certLevel }}</span>
                  </div>
                  <div class="resume-block__desc" v-if="ct.description">{{ ct.description }}</div>
                </div>
              </div>
              <!-- 社团/校园经历 -->
              <div class="resume-block" v-if="detailActivities.length > 0">
                <div class="resume-block__label">社团/校园经历</div>
                <div class="resume-block__item" v-for="(ac, i) in detailActivities" :key="'ac' + i">
                  <div class="resume-block__line">
                    <span class="resume-block__main">{{ ac.orgName || '--' }}</span>
                    <span v-if="ac.position" class="resume-block__tag">{{ ac.position }}</span>
                  </div>
                  <div class="resume-block__desc" v-if="ac.description">{{ ac.description }}</div>
                </div>
              </div>
            </div>
            <!-- 状态流转历史 -->
            <div class="detail-section" v-if="detail.statusHistory && detail.statusHistory.length > 0">
              <h4 class="detail-section__title">状态流转</h4>
              <div class="timeline">
                <div class="timeline-item" v-for="(item, idx) in detail.statusHistory" :key="idx">
                  <div class="timeline-item__dot" :class="idx === 0 ? 'timeline-item__dot--active' : ''"></div>
                  <div class="timeline-item__content">
                    <span class="timeline-item__status">{{ item.statusLabel || statusLabel(item.status) }}</span>
                    <span class="timeline-item__time">{{ formatTime(item.operateTime) }}</span>
                    <span class="timeline-item__remark" v-if="item.remark">{{ item.remark }}</span>
                  </div>
                </div>
              </div>
            </div>
            <!-- 无流转记录 -->
            <div class="detail-section" v-else>
              <h4 class="detail-section__title">状态流转</h4>
              <p class="detail-empty">暂无状态变更记录</p>
            </div>
          </template>
          <div class="message message--error" v-if="detailError">{{ detailError }}</div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import api from '@/utils/axios';
import LoadingSpinner from '@/components/LoadingSpinner.vue';
import { formatDegree } from '@/utils/location';

const applications = ref([]);
const loading = ref(true);
const errorMsg = ref('');
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

// 详情弹窗
const detailVisible = ref(false);
const detailLoading = ref(false);
const detailError = ref('');
const detail = ref(null);

// ── 投递简历（快照）解析 ──
/** 解析快照 JSON 数组字符串；空/非法返回 [] */
function parseJsonList(str) {
  if (!str) return [];
  try {
    const arr = JSON.parse(str);
    return Array.isArray(arr) ? arr : [];
  } catch {
    return [];
  }
}
/** 实习/项目经历类型标签（与 ProfileExtraView 的 recordType 下拉一致） */
const INTERN_LABEL = { I: '实习经历', P: '项目经历' };
/** 技能/证书/语言能力类型标签（与 ProfileExtraView 的 certType 下拉一致） */
const CERT_LABEL = { CERT: '证书', SKILL: '技能', LANGUAGE: '语言能力' };

const detailEducations = computed(() => parseJsonList(detail.value?.snapshotEducations));
const detailInternships = computed(() => parseJsonList(detail.value?.snapshotInternships));
const detailCertificates = computed(() => parseJsonList(detail.value?.snapshotCertificates));
const detailActivities = computed(() => parseJsonList(detail.value?.snapshotActivities));
/** 投递简历区块是否展示（四类至少一类有内容；旧投递快照无三类经历则仅教育有值时也展示） */
const hasResumeContent = computed(() =>
  detailEducations.value.length > 0 ||
  detailInternships.value.length > 0 ||
  detailCertificates.value.length > 0 ||
  detailActivities.value.length > 0
);

function internTypeLabel(code) { return INTERN_LABEL[code] || code || ''; }
function certTypeLabel(code) { return CERT_LABEL[code] || code || ''; }

const STATUS_MAP = {
  PENDING_SCREEN: '待筛选',
  SCREEN_PASSED: '筛选通过',
  SCREEN_FAILED: '筛选未通过',
  WRITTEN_EXAM: '笔试',
  WRITTEN_PASSED: '笔试通过',
  WRITTEN_FAILED: '笔试未通过',
  INTERVIEW: '面试中',
  INTERVIEW_PASSED: '面试通过',
  INTERVIEW_FAILED: '面试未通过',
  OFFER: '已录用',
  ELIMINATED: '已淘汰',
  PENDING: '待筛选',
  REVIEWING: '筛选中',
  REJECTED: '未通过',
};

function statusLabel(s) { return STATUS_MAP[s] || s || '--'; }

function statusClass(s) {
  if (s === 'SCREEN_PASSED' || s === 'WRITTEN_PASSED' || s === 'INTERVIEW_PASSED' || s === 'OFFER') {
    return 'status-tag--success';
  }
  if (s === 'ELIMINATED' || s === 'SCREEN_FAILED' || s === 'WRITTEN_FAILED' || s === 'INTERVIEW_FAILED' || s === 'REJECTED') {
    return 'status-tag--eliminated';
  }
  if (s === 'INTERVIEW' || s === 'WRITTEN_EXAM') {
    return 'status-tag--warning';
  }
  return 'status-tag--pending';
}

function formatTime(timeStr) {
  if (!timeStr) return '-';
  const d = new Date(timeStr);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

async function fetchApplications() {
  loading.value = true;
  errorMsg.value = '';
  try {
    const res = await api.get('/applications/my', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value },
    });
    if (res.code === 200) {
      applications.value = res.data?.rows || [];
      total.value = res.data?.total || 0;
    }
  } catch (e) {
    // 业务/网络错误已由拦截器 toast，这里置空列表并展示内联兜底信息
    applications.value = [];
    total.value = 0;
    errorMsg.value = e.response?.data?.msg || '加载投递记录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}

function goPage(p) {
  if (p < 1 || p > totalPages.value) return;
  pageNum.value = p;
  fetchApplications();
}

async function openDetail(app) {
  detailVisible.value = true;
  detailLoading.value = true;
  detailError.value = '';
  detail.value = null;
  try {
    const res = await api.get(`/applications/${app.applicationId}`);
    if (res.code === 200) {
      detail.value = res.data;
    }
  } catch (e) {
    detailError.value = e.response?.data?.msg || '加载详情失败，请稍后重试';
  } finally {
    detailLoading.value = false;
  }
}

function closeDetail() {
  detailVisible.value = false;
  detail.value = null;
  detailError.value = '';
}

onMounted(() => {
  fetchApplications();
});
</script>

<style scoped>
.applications-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 48px 24px 80px;
}

/* 页头 */
.page-header {
  margin-bottom: 32px;
}
.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: 2px;
  margin-bottom: 8px;
}
.page-subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
}

/* 消息 */
.message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: var(--radius);
  font-size: 14px;
  margin-bottom: 16px;
}
.message--error {
  background: rgba(224, 82, 82, 0.1);
  border: 1px solid rgba(224, 82, 82, 0.25);
  color: #E05252;
}

/* 列表头 */
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.list-header__count {
  font-size: 13px;
  color: var(--color-text-secondary);
}

/* 投递卡片 */
.app-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.app-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: var(--bg-glass);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 18px 22px;
  cursor: pointer;
  transition: all 0.2s;
}
.app-card:hover {
  border-color: rgba(95, 184, 214, 0.5);
  transform: translateX(4px);
}
.app-card__main {
  flex: 1;
  min-width: 0;
}
.app-card__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.app-card__meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}
.app-card__dept,
.app-card__time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.app-card__dept svg,
.app-card__time svg {
  flex-shrink: 0;
  color: #6E7D8A;
}
.app-card__status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
.app-card__chevron {
  color: var(--color-text-secondary);
  transition: transform 0.2s;
}
.app-card:hover .app-card__chevron {
  transform: translateX(3px);
  color: var(--color-primary);
}

/* 状态标签 */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}
.status-tag__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.status-tag--pending {
  background: rgba(95, 184, 214, 0.12);
  color: #5FB8D6;
}
.status-tag--pending .status-tag__dot { background: #5FB8D6; }
.status-tag--success {
  background: rgba(95, 184, 141, 0.12);
  color: #5FB88D;
}
.status-tag--success .status-tag__dot { background: #5FB88D; }
.status-tag--eliminated {
  background: rgba(158, 163, 175, 0.12);
  color: #9CA3AF;
}
.status-tag--eliminated .status-tag__dot { background: #9CA3AF; }
.status-tag--warning {
  background: rgba(232, 163, 61, 0.12);
  color: #E8A33D;
}
.status-tag--warning .status-tag__dot { background: #E8A33D; }

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 24px;
  color: var(--color-text-secondary);
}
.empty-state svg {
  color: var(--color-border);
  margin-bottom: 16px;
}
.empty-state p {
  font-size: 16px;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}
.empty-state span {
  font-size: 13px;
  color: rgba(158, 163, 175, 0.5);
  display: block;
  margin-bottom: 20px;
}
.empty-btn {
  display: inline-block;
  padding: 10px 28px;
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  border-radius: 8px;
  color: #0A0E17;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s;
}
.empty-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(95, 184, 214, 0.3);
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}
.page-btn {
  padding: 8px 20px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  background: transparent;
  color: var(--color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.page-btn:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.page-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
.page-info {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-family: var(--font-mono);
}

/* ====== 弹窗 ====== */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(0, 0, 0, 0.65);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow-y: auto; /* 内容超高时整层可滚，避免卡片顶部被裁出视口 */
}
.modal-card {
  background: var(--bg-glass-strong);
  backdrop-filter: blur(var(--glass-blur-heavy));
  border: 1px solid var(--color-border);
  border-radius: 12px;
  width: 100%;
  max-width: 760px;
  max-height: 80vh;
  overflow-y: auto;
  margin: auto; /* flex 下正常垂直居中；内容超高时 auto margin 归零、顶部贴 0 不裁切 */
  padding: 24px 28px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5);
}
.modal-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.modal-card__header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}
.modal-close {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.modal-close:hover {
  border-color: var(--color-border);
  color: var(--color-text);
}

/* Modal 过渡 */
.modal-enter-active { transition: all 0.25s ease; }
.modal-leave-active { transition: all 0.2s ease; }
.modal-enter-from { opacity: 0; }
.modal-leave-to { opacity: 0; }
.modal-enter-from .modal-card { transform: scale(0.95) translateY(10px); }
.modal-leave-to .modal-card { transform: scale(0.95) translateY(10px); }

/* 详情区块 */
.detail-section {
  margin-bottom: 20px;
}
.detail-section:last-child {
  margin-bottom: 0;
}
.detail-section__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 24px;
}
.detail-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  font-size: 13px;
}
.detail-item__label {
  color: var(--color-text-secondary);
}
.detail-item__value {
  color: var(--color-text);
}
.detail-empty {
  font-size: 13px;
  color: var(--color-text-secondary);
  padding: 12px 0;
}

/* 时间线 */
.timeline {
  position: relative;
  padding-left: 24px;
}
.timeline::before {
  content: '';
  position: absolute;
  left: 7px;
  top: 4px;
  bottom: 4px;
  width: 1px;
  background: var(--color-border);
}
.timeline-item {
  position: relative;
  padding-bottom: 16px;
}
.timeline-item:last-child {
  padding-bottom: 0;
}
.timeline-item__dot {
  position: absolute;
  left: -20px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-border);
  border: 2px solid var(--color-card);
}
.timeline-item__dot--active {
  background: var(--color-primary);
  box-shadow: 0 0 8px rgba(95, 184, 214, 0.4);
}
.timeline-item__content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.timeline-item__status {
  font-size: 14px;
  color: var(--color-text);
  font-weight: 500;
}
.timeline-item__time {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.timeline-item__remark {
  font-size: 12px;
  color: #6E7D8A;
  margin-top: 4px;
  line-height: 1.5;
}

/* 投递简历（快照）区块 */
.resume-section .resume-block {
  margin-bottom: 14px;
}
.resume-block__label {
  font-size: 12px;
  font-weight: 600;
  color: #6E7D8A;
  letter-spacing: 1px;
  margin-bottom: 6px;
}
.resume-block__item {
  padding: 6px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
}
.resume-block__item:last-child {
  border-bottom: none;
}
.resume-block__line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}
.resume-block__main {
  font-size: 13px;
  color: var(--color-text);
  font-weight: 500;
}
.resume-block__tag {
  font-size: 11px;
  color: #5FB8D6;
  background: rgba(95, 184, 214, 0.12);
  border-radius: 4px;
  padding: 1px 6px;
}
.resume-block__sub {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}
.resume-block__desc {
  font-size: 12px;
  color: #6E7D8A;
  margin-top: 2px;
  line-height: 1.5;
  word-break: break-all;
}

/* 响应式 */
@media (max-width: 768px) {
  .applications-page {
    padding: 32px 16px 60px;
  }
  .page-title {
    font-size: 24px;
  }
  .app-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
    padding: 14px 16px;
  }
  .app-card__status {
    width: 100%;
    justify-content: space-between;
  }
  .modal-card {
    max-width: 100%;
    max-height: 90vh;
    padding: 18px 16px;
  }
  .detail-grid {
    grid-template-columns: 1fr;
  }
  }

/* ===== 移动端触摸优化 ===== */
@media (max-width: 767px) {
  .applications-page {
    padding: 24px var(--container-px) 60px;
  }
  .page-title {
    font-size: 22px;
  }

  /* 投递卡片触摸优化 */
  .app-card {
    padding: 16px;
    min-height: 80px;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .app-card__status {
    width: 100%;
    justify-content: space-between;
  }

  /* 详情弹窗底部滑入 */
  .modal-overlay {
    align-items: flex-end;
    padding: 0;
  }
  .modal-card {
    border-radius: 16px 16px 0 0;
    max-width: 100%;
    max-height: 90vh;
    padding: 18px 16px;
  }
  .modal-close {
    width: var(--touch-min);
    height: var(--touch-min);
  }

  /* 详情网格单列 */
  .detail-grid {
    grid-template-columns: 1fr;
  }

  /* 分页按钮触摸优化 */
  .page-btn {
    min-height: var(--touch-min);
    padding: 10px 20px;
  }
}
</style>
