<template>
  <div class="apply-page" v-motion-fade="{ y: 24 }">
    <!-- 加载态 -->
    <LoadingSpinner :visible="loading" text="加载投递信息..." />

    <template v-if="!loading">
      <!-- 页头 -->
      <div class="page-header">
        <h1 class="page-title">投递确认</h1>
        <p class="page-subtitle">请仔细核对岗位信息与个人资料，确认无误后提交</p>
      </div>

      <!-- 错误信息 -->
      <div class="message message--error" v-if="errorMsg">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <span>{{ errorMsg }}</span>
      </div>

      <!-- ====== 岗位摘要卡片 ====== -->
      <div class="section-card" v-if="job">
        <div class="section-card__header">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="7" width="18" height="13" rx="2"/><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          <span>岗位信息</span>
        </div>
        <h2 class="job-title">{{ job.title }}</h2>
        <div class="job-meta">
          <div class="job-meta__item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="7" width="18" height="13" rx="2"/></svg>
            {{ job.deptName || '--' }}
          </div>
          <div class="job-meta__item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="10" r="3"/><path d="M12 21.7C17.3 17 20 13 20 10a8 8 0 1 0-16 0c0 3 2.7 7 8 11.7z"/></svg>
            {{ formatLoc(job.location) || '--' }}
          </div>
          <div class="job-meta__item">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 10v6M2 10l10-5 10 5-10 5z"/></svg>
            {{ formatDegree(job.degreeRequirement) || '--' }}
          </div>
          <div class="job-meta__item job-meta__item--deadline">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            {{ formatDeadline(job.deadline) }}
          </div>
        </div>
        <!-- 岗位描述摘要 -->
        <p class="job-desc" v-if="job.description">{{ truncateText(job.description, 200) }}</p>
      </div>

      <!-- ====== 简历摘要预览 ====== -->
      <div class="section-card" v-if="preview">
        <div class="section-card__header">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          <span>简历摘要</span>
        </div>
        <!-- 个人信息 -->
        <div class="profile-section">
          <h3 class="profile-section__title">基本信息</h3>
          <div class="profile-grid">
            <div class="profile-item">
              <span class="profile-item__label">姓名</span>
              <span class="profile-item__value">{{ preview.name || '--' }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-item__label">性别</span>
              <span class="profile-item__value">{{ preview.gender || '--' }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-item__label">手机号</span>
              <span class="profile-item__value">{{ preview.phone || '--' }}</span>
            </div>
            <div class="profile-item">
              <span class="profile-item__label">邮箱</span>
              <span class="profile-item__value">{{ preview.email || '--' }}</span>
            </div>
          </div>
        </div>
        <!-- 教育背景 -->
        <div class="profile-section" v-if="preview.educationSummary">
          <h3 class="profile-section__title">教育背景</h3>
          <p class="profile-section__text">{{ preview.educationSummary }}</p>
        </div>
        <!-- 附件统计 -->
        <div class="profile-section">
          <h3 class="profile-section__title">简历附件</h3>
          <p class="profile-section__text">
            <template v-if="preview.attachmentCount > 0">
              已上传 <strong>{{ preview.attachmentCount }}</strong> 个附件
            </template>
            <template v-else>
              <span class="text-warning">暂未上传简历附件</span>
              <router-link to="/profile/resume" class="inline-link">去上传</router-link>
            </template>
          </p>
        </div>
        <!-- 资料不全引导 -->
        <div class="profile-warning" v-if="preview.incomplete">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
          <span>部分资料尚未完善，请前往 <router-link to="/profile">个人中心</router-link> 补充完整后再投递。</span>
        </div>
      </div>

      <!-- ====== 选择投递简历附件 ====== -->
      <div class="section-card" v-if="resumeFiles.length > 0">
        <div class="section-card__header">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          <span>选择投递简历（共 {{ resumeFiles.length }} 个附件）</span>
        </div>
        <p class="form-tip" style="margin-bottom:12px">请选择一份简历作为本次投递的正式简历，HR将查看此文件</p>
        <div class="file-radio-list">
          <label
            v-for="f in resumeFiles"
            :key="f.id"
            class="file-radio-item"
            :class="{ 'file-radio-item--selected': selectedFileId === f.id }"
          >
            <input
              type="radio"
              :value="f.id"
              v-model="selectedFileId"
              class="file-radio-input"
            />
            <div class="file-radio-info">
              <span class="file-radio-name">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
                {{ f.originalName }}
              </span>
              <span class="file-radio-meta">{{ formatFileSize(f.fileSize) }} · {{ f.createTime?.slice(0, 10) }}</span>
            </div>
            <span class="file-radio-check" v-if="selectedFileId === f.id">&#10003;</span>
          </label>
        </div>
        <p class="form-tip form-tip--error" v-if="resumeFiles.length > 0 && !selectedFileId" style="margin-top:8px">请选择一份简历附件用于投递</p>
      </div>

      <!-- ====== 渠道来源 + 确认投递 ====== -->
      <div class="section-card" v-if="job">
        <div class="section-card__header">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>
          <span>确认投递</span>
        </div>
        <div class="form-group">
          <label class="form-label">渠道来源</label>
          <select v-model="source" class="form-select">
            <option value="官网">官网</option>
            <option value="宣讲会">宣讲会</option>
            <option value="内推">内推</option>
            <option value="其他">其他</option>
          </select>
        </div>
        <p class="apply-notice">
          提交后信息无法撤回，请确认以上信息真实有效。虚假信息将取消应聘资格。
        </p>
        <button
          class="btn-submit"
          @click="handleApply"
          :disabled="submitting || (preview && preview.incomplete)"
        >
          <template v-if="submitting">
            <span class="btn-spinner"></span> 提交中...
          </template>
          <template v-else>
            确认投递
          </template>
        </button>
        <p class="form-tip" v-if="preview && preview.incomplete">
          请先完善个人资料后再投递
        </p>
      </div>

      <!-- 非预期状态：岗位不存在 -->
      <div class="empty-state" v-if="!job && !errorMsg">
        <p>未找到岗位信息</p>
        <router-link to="/jobs" class="empty-link">返回岗位列表</router-link>
      </div>
    </template>

    <!-- Toast 提示 -->
    <Transition name="toast">
      <div class="toast" v-if="toast.show">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        {{ toast.message }}
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/utils/axios';
import { formatLoc, formatDegree } from '@/utils/location';
import LoadingSpinner from '@/components/LoadingSpinner.vue';

const route = useRoute();
const router = useRouter();

const loading = ref(true);
const submitting = ref(false);
const job = ref(null);
const preview = ref(null);
const source = ref('官网');
const resumeFiles = ref([]);
const selectedFileId = ref(null);
const errorMsg = ref('');
const toast = ref({ show: false, message: '' });

function formatDeadline(deadline) {
  if (!deadline) return '--';
  const d = new Date(deadline);
  const now = new Date();
  const diff = Math.ceil((d - now) / (1000 * 60 * 60 * 24));
  if (diff < 0) return '已截止';
  if (diff === 0) return '今日截止';
  if (diff <= 7) return `${diff}天后截止`;
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()} 截止`;
}

function truncateText(text, len) {
  if (!text) return '';
  const plain = text.replace(/<[^>]+>/g, '');
  return plain.length > len ? plain.slice(0, len) + '...' : plain;
}

function showToast(message, duration = 2500) {
  toast.value = { show: true, message };
  setTimeout(() => { toast.value.show = false; }, duration);
}

function formatFileSize(bytes) {
  if (!bytes) return '--';
  if (bytes < 1024) return bytes + 'B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB';
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB';
}

async function fetchPreview() {
  loading.value = true;
  errorMsg.value = '';

  const jobId = route.params.jobId;
  if (!jobId) {
    errorMsg.value = '缺少岗位ID参数';
    loading.value = false;
    return;
  }

  // 1. 获取岗位信息（使用已验证可用的 /jobs/{id} 端点）
  try {
    const jobRes = await api.get(`/jobs/${jobId}`);
    if (jobRes.code === 200 && jobRes.data) {
      job.value = jobRes.data;
    }
  } catch (e) {
    errorMsg.value = e.response?.data?.msg || '获取岗位信息失败，请检查网络连接';
    loading.value = false;
    return;
  }

  // 2. 获取简历预览数据（非关键路径：失败不影响岗位展示，仅降级隐藏简历卡片）
  try {
    const [profileRes, eduRes, filesRes] = await Promise.all([
      api.get('/profile').catch(() => ({ code: 0 })),
      api.get('/profile/education').catch(() => ({ code: 0 })),
      api.get('/files').catch(() => ({ code: 0 })),
    ]);

    const profile = (profileRes.code === 200 && profileRes.data) ? profileRes.data : null;

    let educationSummary = '';
    if (eduRes.code === 200 && eduRes.data && eduRes.data.length > 0) {
      const latest = eduRes.data[0];
      educationSummary = [latest.schoolName, latest.major, latest.degree]
        .filter(Boolean).join(' / ');
    }

    preview.value = {
      name: profile?.name || '',
      gender: profile?.gender || '',
      phone: profile?.phone || '',
      email: profile?.email || '',
      educationSummary,
      attachmentCount: (filesRes.code === 200 && filesRes.data) ? filesRes.data.length : 0,
      incomplete: !profile?.name || !profile?.phone || !profile?.email,
    };
  } catch {
    // 简历接口全部异常时，隐藏简历卡片
    preview.value = null;
  }

  loading.value = false;
}

async function handleApply() {
  errorMsg.value = '';
  submitting.value = true;
  try {
    const res = await api.post('/applications/submit', {
      jobId: String(route.params.jobId),
      source: source.value,
      fileId: selectedFileId.value,
    });
    if (res.code === 200) {
      showToast('投递成功！');
      setTimeout(() => {
        router.push('/my-applications');
      }, 1500);
    } else {
      errorMsg.value = res.message || '投递失败，请重试';
    }
  } catch (e) {
    const data = e.response?.data;
    const code = data?.code;
    if (code === 50001 || data?.message?.includes('已投递')) {
      errorMsg.value = '您已投递过该岗位，不能重复投递。';
    } else if (code === 40003 || code === 50002 || data?.message?.includes('截止')) {
      errorMsg.value = '该岗位已截止投递。';
    } else if (code === 30002 || code === 30003 || code === 30004 || data?.message?.includes('资料不完整') || data?.message?.includes('完善')) {
      errorMsg.value = '您的个人资料不完整，请前往个人中心完善后再投递。';
    } else {
      errorMsg.value = data?.message || '投递失败，请稍后重试';
    }
  } finally {
    submitting.value = false;
  }
}

onMounted(async () => {
  fetchPreview();
  // 拉取简历附件列表
  try {
    const filesRes = await api.get('/files');
    if (filesRes.code === 200) {
      resumeFiles.value = filesRes.data || [];
      if (resumeFiles.value.length > 0) {
        selectedFileId.value = resumeFiles.value[0].id; // 默认选中第一个
      }
    }
  } catch {}
});
</script>

<style scoped>
.apply-page {
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

/* 区块卡片 */
.section-card {
  background: var(--bg-glass);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 24px 28px;
  margin-bottom: 16px;
}
.section-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}
.section-card__header svg {
  color: var(--color-primary);
}
.section-card__header span {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

/* 岗位信息 */
.job-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 12px;
}
.job-meta {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.job-meta__item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
.job-meta__item svg {
  flex-shrink: 0;
  color: #6E7D8A;
}
.job-meta__item--deadline {
  color: var(--color-primary);
}
.job-meta__item--deadline svg {
  color: var(--color-primary);
}
.job-desc {
  font-size: 13px;
  color: #6E7D8A;
  line-height: 1.7;
}

/* 简历摘要 */
.profile-section {
  margin-bottom: 16px;
}
.profile-section:last-child {
  margin-bottom: 0;
}
.profile-section__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 10px;
}
.profile-section__text {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}
.profile-section__text strong {
  color: var(--color-primary);
}
.text-warning {
  color: var(--color-warning);
}
.inline-link {
  color: var(--color-primary);
  margin-left: 8px;
  font-size: 13px;
}
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 32px;
}
.profile-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
  font-size: 14px;
}
.profile-item__label {
  color: var(--color-text-secondary);
}
.profile-item__value {
  color: var(--color-text);
}

/* 资料不全警告 */
.profile-warning {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 14px;
  background: rgba(232, 163, 61, 0.08);
  border: 1px solid rgba(232, 163, 61, 0.2);
  border-radius: var(--radius);
  font-size: 13px;
  color: var(--color-warning);
  line-height: 1.6;
}
.profile-warning svg {
  flex-shrink: 0;
  color: var(--color-warning);
}
.profile-warning a {
  color: var(--color-primary);
  text-decoration: underline;
}

/* 表单 */
.form-group {
  margin-bottom: 16px;
}
.form-label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 8px;
}
.form-select {
  width: 100%;
  max-width: 320px;
  padding: 10px 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: var(--color-text);
  font-size: 14px;
  font-family: inherit;
  cursor: pointer;
  outline: none;
  transition: border-color 0.2s;
  appearance: auto;
}
.form-select:focus {
  border-color: var(--color-primary);
}
.form-select option {
  background: var(--color-card);
  color: var(--color-text);
}

/* 投递提示 */
.apply-notice {
  font-size: 13px;
  color: var(--color-warning);
  padding: 10px 14px;
  background: rgba(232, 163, 61, 0.06);
  border: 1px solid rgba(232, 163, 61, 0.15);
  border-radius: var(--radius);
  margin-bottom: 20px;
  line-height: 1.6;
}

/* 提交按钮 */
.btn-submit {
  width: 100%;
  padding: 14px 24px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  color: #0A0E17;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  letter-spacing: 1px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: inherit;
}
.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(95, 184, 214, 0.3);
}
.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(10, 14, 23, 0.3);
  border-top-color: #0A0E17;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.form-tip {
  text-align: center;
  font-size: 13px;
  color: var(--color-warning);
  margin-top: 10px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 24px;
  color: var(--color-text-secondary);
}
.empty-state p {
  font-size: 16px;
  margin-bottom: 12px;
}
.empty-link {
  color: var(--color-primary);
  font-size: 14px;
}

/* Toast */
.toast {
  position: fixed;
  top: 88px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  background: linear-gradient(135deg, #152535, #1a3a4a);
  border: 1px solid var(--color-success);
  border-radius: 10px;
  color: var(--color-success);
  font-size: 15px;
  font-weight: 600;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  letter-spacing: 1px;
}
.toast svg {
  color: var(--color-success);
  flex-shrink: 0;
}

.toast-enter-active { transition: all 0.3s ease; }
.toast-leave-active { transition: all 0.3s ease; }
.toast-enter-from { opacity: 0; transform: translateX(-50%) translateY(-12px); }
.toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(-12px); }

/* 响应式 */
@media (max-width: 767px) {
  .apply-page {
    padding: 24px 16px 100px;
  }
  .page-title {
    font-size: 24px;
  }
  .section-card {
    padding: 16px;
  }
  .job-meta {
    gap: 12px;
  }
  .profile-grid {
    grid-template-columns: 1fr;
  }
  .form-select {
    max-width: 100%;
    min-height: var(--input-min-h);
    font-size: 16px;
  }
  .btn-submit {
    min-height: var(--input-min-h);
    font-size: 16px;
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 90;
    border-radius: 0;
  }
  .file-radio-item {
    padding: 14px 16px;
    min-height: 56px;
  }
}

/* 附件单选列表 */
.file-radio-list { display: flex; flex-direction: column; gap: 8px; }
.file-radio-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-radius: 8px; border: 1px solid var(--color-border);
  cursor: pointer; transition: all 0.2s;
}
.file-radio-item:hover { border-color: var(--color-primary); }
.file-radio-item--selected { border-color: var(--color-primary); background: rgba(95,184,214,0.06); }
.file-radio-input { accent-color: var(--color-primary); width: 16px; height: 16px; }
.file-radio-info { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.file-radio-name { display: flex; align-items: center; gap: 6px; font-size: 14px; color: var(--color-text); }
.file-radio-name svg { flex-shrink: 0; color: var(--color-text-secondary); }
.file-radio-meta { font-size: 12px; color: var(--color-text-secondary); }
.file-radio-check { color: var(--color-primary); font-size: 18px; font-weight: 700; }
.form-tip--error { color: #E05252; }
</style>
