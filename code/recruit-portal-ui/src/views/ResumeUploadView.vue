<template>
  <div class="resume-page">
    <div class="page-header">
      <h1 class="page-title">简历附件</h1>
      <p class="page-subtitle">管理您的简历文件，上传后可投递岗位时使用</p>
    </div>

    <!-- 上传区域 -->
    <div
      class="upload-zone"
      :class="{ 'upload-zone--dragover': dragOver, 'upload-zone--uploading': uploading }"
      @click="triggerUpload"
      @dragover.prevent="dragOver = true"
      @dragleave.prevent="dragOver = false"
      @drop.prevent="handleDrop"
    >
      <input
        type="file"
        ref="fileInput"
        accept=".pdf,.doc,.docx"
        @change="handleFileChange"
        hidden
      />
      <div class="upload-zone__inner">
        <!-- 上传图标 -->
        <svg class="upload-zone__icon" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
          <polyline points="17 8 12 3 7 8"/>
          <line x1="12" y1="3" x2="12" y2="15"/>
        </svg>
        <p class="upload-zone__text" v-if="!uploading">
          <span class="upload-zone__link">点击选择文件</span> 或将文件拖拽到此处
        </p>
        <!-- 上传进度 -->
        <div class="upload-progress" v-if="uploading">
          <p class="upload-progress__text">上传中... {{ uploadProgress }}%</p>
          <div class="upload-progress__bar">
            <div class="upload-progress__fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
        </div>
        <p class="upload-zone__hint">
          支持 PDF、Word 格式，单个文件不超过 10MB。推荐上传 PDF 格式，可即时预览。
        </p>
      </div>
    </div>

    <!-- 错误信息 -->
    <div class="message message--error" v-if="errorMsg">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
      {{ errorMsg }}
    </div>

    <!-- 加载中 -->
    <LoadingSpinner :visible="loading" text="加载文件列表..." />

    <!-- 空状态 -->
    <div class="empty-state" v-if="!loading && files.length === 0 && !errorMsg">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
        <polyline points="14 2 14 8 20 8"/>
      </svg>
      <p>暂无简历附件</p>
      <span>上传您的简历文件，支持 PDF、Word 格式</span>
    </div>

    <!-- 文件列表 -->
    <div class="file-list" v-if="!loading && files.length > 0">
      <div class="list-header">
        <h3 class="list-header__title">已上传文件（{{ files.length }}）</h3>
      </div>
      <div class="file-card" v-for="f in files" :key="f.id">
        <!-- 文件图标 -->
        <div class="file-card__icon" :class="'file-card__icon--' + fileIconType(f.originalName)">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
        </div>
        <!-- 文件信息 -->
        <div class="file-card__info">
          <span class="file-card__name" :title="f.originalName">{{ f.originalName }}</span>
          <span class="file-card__meta">
            {{ formatSize(f.fileSize) }} · {{ formatTime(f.createTime) }}
          </span>
        </div>
        <!-- 转换状态标签 -->
        <div class="file-card__status">
          <span class="status-badge" :class="statusClass(f.previewStatus)">
            <span class="status-badge__dot"></span>
            {{ statusLabel(f.previewStatus) }}
          </span>
          <!-- Word 转换失败时提示推荐 PDF -->
          <span class="convert-tip" v-if="f.previewStatus === 'FAILED' && isWordFile(f.originalName)">
            推荐上传 <strong>PDF</strong> 格式以获得更好的预览体验
          </span>
        </div>
        <!-- 操作按钮 -->
        <div class="file-card__actions">
          <button
            class="action-btn"
            title="预览"
            @click="previewFile(f)"
            :disabled="f.previewStatus === 'FAILED'"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
          </button>
          <button
            class="action-btn"
            title="下载"
            @click="downloadFile(f)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          </button>
          <button
            class="action-btn action-btn--danger"
            title="删除"
            @click="deleteFile(f)"
            :disabled="deleting === f.id"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- PDF 预览弹窗 -->
    <Transition name="modal">
      <div class="modal-overlay" v-if="previewVisible" @click.self="closePreview">
        <div class="modal-card modal-card--wide">
          <div class="modal-card__header">
            <h3>{{ previewFileName }}</h3>
            <button class="modal-close" @click="closePreview">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          <LoadingSpinner :visible="previewLoading" text="加载预览..." />
          <div class="message message--error" v-if="previewError">{{ previewError }}</div>
          <iframe
            v-if="previewUrl && !previewLoading && !previewError"
            :src="previewUrl"
            class="preview-frame"
            frameborder="0"
          ></iframe>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import api from '@/utils/axios';
import LoadingSpinner from '@/components/LoadingSpinner.vue';

const fileInput = ref(null);
const dragOver = ref(false);
const uploading = ref(false);
const uploadProgress = ref(0);
const loading = ref(true);
const deleting = ref(null);
const files = ref([]);
const errorMsg = ref('');

// PDF 预览弹窗
const previewVisible = ref(false);
const previewLoading = ref(false);
const previewError = ref('');
const previewUrl = ref('');
const previewFileName = ref('');

const STATUS_MAP = {
  PENDING: '处理中',
  SUCCESS: '可预览',
  FAILED: '预览失败',
  NONE: '可预览',
  READY: '可预览',
};

function statusLabel(s) { return STATUS_MAP[s] || s || '处理中'; }

function statusClass(s) {
  if (s === 'SUCCESS') return 'status-badge--success';
  if (s === 'FAILED') return 'status-badge--failed';
  return 'status-badge--pending';
}

function fileIconType(fileName) {
  if (!fileName) return 'pdf';
  const ext = fileName.split('.').pop().toLowerCase();
  if (ext === 'pdf') return 'pdf';
  if (ext === 'doc' || ext === 'docx') return 'word';
  return 'file';
}

/** 判断是否为 Word 文件 */
function isWordFile(fileName) {
  if (!fileName) return false;
  const ext = fileName.split('.').pop().toLowerCase();
  return ext === 'doc' || ext === 'docx';
}

function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function formatTime(timeStr) {
  if (!timeStr) return '-';
  const d = new Date(timeStr);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function triggerUpload() {
  if (uploading.value) return;
  fileInput.value?.click();
}

function handleFileChange(e) {
  const file = e.target.files?.[0];
  if (file) doUpload(file);
}

function handleDrop(e) {
  dragOver.value = false;
  const file = e.dataTransfer?.files?.[0];
  if (file) doUpload(file);
}

async function doUpload(file) {
  // 客户端校验
  if (file.size > 10 * 1024 * 1024) {
    errorMsg.value = '文件大小超过 10MB 限制，请压缩后重试';
    return;
  }
  const allowedExts = ['.pdf', '.doc', '.docx'];
  const ext = '.' + file.name.split('.').pop().toLowerCase();
  if (!allowedExts.includes(ext)) {
    errorMsg.value = '不支持的文件格式，请上传 PDF、DOC 或 DOCX 文件';
    return;
  }

  errorMsg.value = '';
  uploading.value = true;
  uploadProgress.value = 0;
  try {
    const formData = new FormData();
    formData.append('file', file);
    await api.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total) {
          uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total);
        }
      },
    });
    // 上传成功后刷新列表
    await fetchFiles();
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '上传失败，请重试';
  } finally {
    uploading.value = false;
    uploadProgress.value = 0;
    // 重置 file input 以允许重复上传同一文件
    if (fileInput.value) fileInput.value.value = '';
  }
}

async function fetchFiles() {
  loading.value = true;
  try {
    const res = await api.get('/files');
    // 后端返回 { code: 200, data: [...] } 或直接 data 为数组
    files.value = Array.isArray(res.data) ? res.data : (res.data?.rows || []);
  } catch {
    files.value = [];
  } finally {
    loading.value = false;
  }
}

/** 内嵌 PDF 预览 -- 通过 fetch 带 Authorization header 获取文件 blob，生成 blob URL 供 iframe 使用 */
async function previewFile(f) {
  previewVisible.value = true;
  previewFileName.value = f.originalName;
  previewLoading.value = true;
  previewError.value = '';
  previewUrl.value = '';

  // 释放之前的 blob URL
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value);
  }

  try {
    const token = localStorage.getItem('access_token');
    const response = await axios.get(`/api/portal/files/${f.id}/preview`, {
      responseType: 'blob',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    const blob = response.data;
    previewUrl.value = URL.createObjectURL(blob);
  } catch (e) {
    previewError.value = '加载预览失败，请稍后重试或下载文件查看';
  } finally {
    previewLoading.value = false;
  }
}

function closePreview() {
  previewVisible.value = false;
  if (previewUrl.value && previewUrl.value.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl.value);
  }
  previewUrl.value = '';
  previewFileName.value = '';
  previewError.value = '';
}

function downloadFile(f) {
  // 触发文件下载
  window.open(`/api/portal/files/${f.id}/download`, '_blank');
}

async function deleteFile(f) {
  if (!confirm(`确认删除文件「${f.originalName}」？此操作不可撤销。`)) return;
  deleting.value = f.id;
  errorMsg.value = '';
  try {
    await api.delete(`/files/${f.id}`);
    files.value = files.value.filter(item => item.id !== f.id);
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '删除失败，请重试';
  } finally {
    deleting.value = null;
  }
}

onMounted(() => {
  fetchFiles();
});
</script>

<style scoped>
.resume-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 48px 24px 80px;
}

/* 页面标题 */
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

/* 上传区域 */
.upload-zone {
  border: 2px dashed var(--color-border);
  border-radius: 12px;
  padding: 0;
  cursor: pointer;
  transition: all 0.3s ease;
  background: rgba(21, 37, 53, 0.4);
  margin-bottom: 24px;
}
.upload-zone:hover {
  border-color: var(--color-primary);
  background: rgba(95, 184, 214, 0.06);
}
.upload-zone--dragover {
  border-color: var(--color-primary);
  background: rgba(95, 184, 214, 0.1);
  transform: scale(1.01);
}
.upload-zone--uploading {
  pointer-events: none;
  opacity: 0.7;
}
.upload-zone__inner {
  padding: 48px 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.upload-zone__icon {
  color: var(--color-border);
  transition: color 0.3s;
}
.upload-zone:hover .upload-zone__icon {
  color: var(--color-primary);
}
.upload-zone__text {
  font-size: 15px;
  color: var(--color-text-secondary);
}
.upload-zone__link {
  color: var(--color-primary);
  text-decoration: underline;
  cursor: pointer;
}
.upload-zone__hint {
  font-size: 13px;
  color: rgba(158, 163, 175, 0.6);
  max-width: 420px;
  line-height: 1.6;
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
}

/* 文件列表 */
.file-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.list-header__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

/* 文件卡片 */
.file-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  margin-bottom: 8px;
  transition: all 0.2s;
}
.file-card:hover {
  border-color: rgba(95, 184, 214, 0.5);
}
.file-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.file-card__icon--pdf {
  background: rgba(224, 82, 82, 0.12);
  color: #E05252;
}
.file-card__icon--word {
  background: rgba(95, 184, 214, 0.12);
  color: var(--color-primary);
}
.file-card__icon--file {
  background: rgba(158, 163, 175, 0.12);
  color: var(--color-text-secondary);
}
.file-card__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.file-card__name {
  font-size: 14px;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-card__meta {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 状态标签 */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}
.status-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}
.status-badge--pending {
  background: rgba(95, 184, 214, 0.12);
  color: #5FB8D6;
}
.status-badge--pending .status-badge__dot {
  background: #5FB8D6;
  animation: pulse 1.5s ease-in-out infinite;
}
.status-badge--success {
  background: rgba(95, 184, 141, 0.12);
  color: #5FB88D;
}
.status-badge--success .status-badge__dot {
  background: #5FB88D;
}
.status-badge--failed {
  background: rgba(224, 82, 82, 0.12);
  color: #E05252;
}
.status-badge--failed .status-badge__dot {
  background: #E05252;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* 操作按钮 */
.file-card__actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
.action-btn {
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
.action-btn:hover:not(:disabled) {
  background: rgba(95, 184, 214, 0.1);
  border-color: var(--color-border);
  color: var(--color-primary);
}
.action-btn--danger:hover:not(:disabled) {
  background: rgba(224, 82, 82, 0.1);
  border-color: rgba(224, 82, 82, 0.3);
  color: #E05252;
}
.action-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 768px) {
  .resume-page {
    padding: 32px 16px 60px;
  }
  .page-title {
    font-size: 24px;
  }
  .upload-zone__inner {
    padding: 32px 16px;
  }
  .file-card {
    flex-wrap: wrap;
    gap: 10px;
    padding: 14px 16px;
  }
  .file-card__info {
    flex: 1 1 calc(100% - 100px);
  }
  .file-card__status {
    order: 1;
  }
  .file-card__actions {
    order: 1;
  }
  .modal-card--wide {
    max-width: 100%;
    max-height: 90vh;
    padding: 18px 16px;
  }
  .preview-frame {
    height: 60vh;
  }
}

/* ====== 上传进度条 ====== */
.upload-progress {
  width: 100%;
  max-width: 320px;
  text-align: center;
}
.upload-progress__text {
  font-size: 14px;
  color: var(--color-primary);
  margin-bottom: 8px;
}
.upload-progress__bar {
  width: 100%;
  height: 6px;
  background: var(--color-border);
  border-radius: 3px;
  overflow: hidden;
}
.upload-progress__fill {
  height: 100%;
  background: linear-gradient(90deg, #5FB8D6, #6BB3FF);
  border-radius: 3px;
  transition: width 0.3s ease;
}

/* ====== Word 转换失败提示 ====== */
.convert-tip {
  display: block;
  font-size: 11px;
  color: #E8A33D;
  margin-top: 4px;
  max-width: 180px;
  line-height: 1.4;
}
.convert-tip strong {
  color: #F0C060;
}

/* ====== 预览弹窗 ====== */
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
}
.modal-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  width: 100%;
  max-width: 560px;
  max-height: 80vh;
  overflow-y: auto;
  padding: 24px 28px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.5);
}
.modal-card--wide {
  max-width: 900px;
}
.modal-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.modal-card__header h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 12px;
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
  flex-shrink: 0;
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

/* 预览 iframe */
.preview-frame {
  width: 100%;
  height: 70vh;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  background: #fff;
}
</style>
