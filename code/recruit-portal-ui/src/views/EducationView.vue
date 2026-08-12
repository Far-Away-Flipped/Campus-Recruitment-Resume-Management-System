<template>
  <div class="education-page">
    <div class="page-header">
      <h1 class="page-title">教育经历</h1>
      <button class="btn-add" @click="openDialog()">+ 新增教育经历</button>
    </div>

    <!-- Toast 提示 -->
    <div class="form-toast form-toast--success" v-if="toast.success">{{ toast.success }}</div>
    <div class="form-toast form-toast--error" v-if="toast.error">{{ toast.error }}</div>

    <!-- 加载中 -->
    <LoadingSpinner :visible="loading" text="加载教育经历..." />

    <!-- 空状态 -->
    <div class="empty" v-if="!loading && list.length === 0">
      <p>暂无教育经历，请点击上方按钮添加。</p>
    </div>

    <!-- 教育经历列表 -->
    <div class="edu-list" v-if="!loading && list.length > 0">
      <div class="edu-card" v-for="item in list" :key="item.id">
        <div class="edu-card__header">
          <h3 class="edu-card__school">{{ item.schoolName || '-' }}</h3>
          <span class="edu-card__degree">{{ degreeLabel(item.degree) }}</span>
        </div>
        <div class="edu-card__body">
          <div class="edu-card__field">
            <span class="edu-card__label">专业</span>
            <span class="edu-card__value">{{ item.major || '-' }}</span>
          </div>
          <div class="edu-card__field">
            <span class="edu-card__label">入学时间</span>
            <span class="edu-card__value">{{ item.startDate || '-' }}</span>
          </div>
          <div class="edu-card__field">
            <span class="edu-card__label">毕业时间</span>
            <span class="edu-card__value">{{ item.endDate || '-' }}</span>
          </div>
          <div class="edu-card__field" v-if="item.gpa">
            <span class="edu-card__label">GPA</span>
            <span class="edu-card__value">{{ item.gpa }}</span>
          </div>
        </div>
        <div class="edu-card__actions">
          <button class="btn-action btn-action--edit" @click="openDialog(item)">编辑</button>
          <button class="btn-action btn-action--delete" @click="handleDelete(item.id)">删除</button>
        </div>
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <div class="dialog-overlay" v-if="dialogVisible" @click.self="closeDialog">
      <div class="dialog-card">
        <h2 class="dialog-title">{{ editingId ? '编辑教育经历' : '新增教育经历' }}</h2>

        <!-- 表单错误 -->
        <div class="form-toast form-toast--error" v-if="dialogError">{{ dialogError }}</div>

        <form @submit.prevent="handleSave">
          <!-- 学校名称 -->
          <div class="form-group">
            <label>学校名称 <span class="required">*</span></label>
            <input
              v-model="dialogForm.schoolName"
              type="text"
              placeholder="请输入学校名称"
            />
          </div>

          <!-- 专业 -->
          <div class="form-group">
            <label>专业 <span class="required">*</span></label>
            <input
              v-model="dialogForm.major"
              type="text"
              placeholder="请输入专业名称"
            />
          </div>

          <!-- 学历 -->
          <div class="form-group">
            <label>学历 <span class="required">*</span></label>
            <select v-model="dialogForm.degree">
              <option value="">请选择学历</option>
              <option value="BACHELOR">本科</option>
              <option value="MASTER">硕士</option>
              <option value="DOCTOR">博士</option>
              <option value="OTHER">其他</option>
            </select>
          </div>

          <!-- 入学 / 毕业时间 -->
          <div class="form-row">
            <div class="form-group">
              <label>入学时间 <span class="required">*</span></label>
              <input v-model="dialogForm.startDate" type="date" />
            </div>
            <div class="form-group">
              <label>毕业时间 <span class="required">*</span></label>
              <input v-model="dialogForm.endDate" type="date" />
            </div>
          </div>

          <!-- GPA -->
          <div class="form-group">
            <label>GPA <span class="optional">（选填）</span></label>
            <input
              v-model="dialogForm.gpa"
              type="text"
              placeholder="例如：3.8/4.0"
            />
          </div>

          <!-- 按钮 -->
          <div class="dialog-buttons">
            <button type="button" class="btn-cancel" @click="closeDialog">取消</button>
            <button type="submit" class="btn-submit" :disabled="saving">
              <span class="btn-spinner" v-if="saving"></span>
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import api from '../utils/axios.js';
import LoadingSpinner from '../components/LoadingSpinner.vue';

// --- 列表状态 ---
const list = ref([]);
const loading = ref(true);
const toast = reactive({ success: '', error: '' });

// --- 弹窗状态 ---
const dialogVisible = ref(false);
const editingId = ref(null);
const saving = ref(false);
const dialogError = ref('');

const emptyForm = () => ({
  schoolName: '',
  major: '',
  degree: '',
  startDate: '',
  endDate: '',
  gpa: '',
});

const dialogForm = reactive(emptyForm());

/** 学历映射 */
const DEGREE_MAP = {
  BACHELOR: '本科',
  MASTER: '硕士',
  DOCTOR: '博士',
  OTHER: '其他',
};

function degreeLabel(key) {
  return DEGREE_MAP[key] || key || '-';
}

/** 加载教育经历列表 */
async function loadList() {
  loading.value = true;
  try {
    const res = await api.get('/profile/education');
    // res.data 可能是数组或 { rows: [...] } — 兼容两种返回格式
    list.value = Array.isArray(res.data) ? res.data : (res.data?.rows || []);
  } catch (e) {
    toast.error = e.response?.data?.msg || '加载教育经历失败';
  } finally {
    loading.value = false;
  }
}

/** 打开弹窗 — 新增或编辑 */
function openDialog(item = null) {
  dialogError.value = '';
  if (item) {
    editingId.value = item.id;
    dialogForm.schoolName = item.schoolName || '';
    dialogForm.major = item.major || '';
    dialogForm.degree = item.degree || '';
    dialogForm.startDate = item.startDate || '';
    dialogForm.endDate = item.endDate || '';
    dialogForm.gpa = item.gpa || '';
  } else {
    editingId.value = null;
    Object.assign(dialogForm, emptyForm());
  }
  dialogVisible.value = true;
}

/** 关闭弹窗 */
function closeDialog() {
  dialogVisible.value = false;
  editingId.value = null;
  dialogError.value = '';
}

/** 弹窗表单校验 */
function validateDialog() {
  dialogError.value = '';
  if (!dialogForm.schoolName.trim()) {
    dialogError.value = '请输入学校名称';
    return false;
  }
  if (!dialogForm.major.trim()) {
    dialogError.value = '请输入专业名称';
    return false;
  }
  if (!dialogForm.degree) {
    dialogError.value = '请选择学历';
    return false;
  }
  if (!dialogForm.startDate) {
    dialogError.value = '请选择入学时间';
    return false;
  }
  if (!dialogForm.endDate) {
    dialogError.value = '请选择毕业时间';
    return false;
  }
  if (dialogForm.startDate >= dialogForm.endDate) {
    dialogError.value = '入学时间不能晚于毕业时间';
    return false;
  }
  return true;
}

/** 保存（新增或编辑） */
async function handleSave() {
  if (!validateDialog()) return;

  saving.value = true;
  try {
    const payload = {
      schoolName: dialogForm.schoolName.trim(),
      major: dialogForm.major.trim(),
      degree: dialogForm.degree,
      startDate: dialogForm.startDate,
      endDate: dialogForm.endDate,
      gpa: dialogForm.gpa || undefined,
    };

    if (editingId.value) {
      await api.put(`/profile/education/${editingId.value}`, payload);
      toast.success = '教育经历已更新';
    } else {
      await api.post('/profile/education', payload);
      toast.success = '教育经历已添加';
    }

    closeDialog();
    await loadList();
    setTimeout(() => (toast.success = ''), 3000);
  } catch (e) {
    dialogError.value = e.response?.data?.msg || '保存失败，请稍后重试';
  } finally {
    saving.value = false;
  }
}

/** 删除 */
async function handleDelete(id) {
  if (!confirm('确定要删除这条教育经历吗？')) return;

  try {
    await api.delete(`/profile/education/${id}`);
    toast.success = '教育经历已删除';
    await loadList();
    setTimeout(() => (toast.success = ''), 3000);
  } catch (e) {
    toast.error = e.response?.data?.msg || '删除失败，请稍后重试';
    setTimeout(() => (toast.error = ''), 3000);
  }
}

onMounted(() => {
  loadList();
});
</script>

<style scoped>
.education-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 24px;
}

/* 页头 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.page-title {
  font-size: 28px;
  color: var(--color-text);
}
.btn-add {
  padding: 10px 20px;
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  border: none;
  border-radius: var(--radius);
  color: var(--color-bg);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
}
.btn-add:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

/* Toast */
.form-toast {
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 16px;
}
.form-toast--error {
  background: rgba(224, 82, 82, 0.12);
  color: var(--color-danger);
  border: 1px solid rgba(224, 82, 82, 0.25);
}
.form-toast--success {
  background: rgba(95, 184, 141, 0.12);
  color: var(--color-success);
  border: 1px solid rgba(95, 184, 141, 0.25);
}

/* 空状态 */
.empty {
  text-align: center;
  padding: 60px 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

/* 列表卡片 */
.edu-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.edu-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 24px;
  transition: border-color 0.2s;
}
.edu-card:hover {
  border-color: rgba(95, 184, 214, 0.5);
}
.edu-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.edu-card__school {
  font-size: 18px;
  color: var(--color-text);
  font-weight: 600;
}
.edu-card__degree {
  font-size: 12px;
  padding: 3px 10px;
  background: rgba(95, 184, 214, 0.12);
  color: var(--color-primary);
  border-radius: 4px;
  flex-shrink: 0;
}
.edu-card__body {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 24px;
  margin-bottom: 16px;
}
.edu-card__field {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.04);
}
.edu-card__label {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.edu-card__value {
  font-size: 13px;
  color: var(--color-text);
}
.edu-card__actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}
.btn-action {
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: transparent;
  transition: all 0.2s;
}
.btn-action--edit {
  color: var(--color-primary);
  border-color: rgba(95, 184, 214, 0.4);
}
.btn-action--edit:hover {
  background: rgba(95, 184, 214, 0.1);
}
.btn-action--delete {
  color: var(--color-danger);
  border-color: rgba(224, 82, 82, 0.4);
}
.btn-action--delete:hover {
  background: rgba(224, 82, 82, 0.1);
}

/* 弹窗遮罩 */
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 200;
  background: rgba(10, 14, 23, 0.75);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.dialog-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 32px;
  width: 100%;
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
}
.dialog-title {
  font-size: 20px;
  margin-bottom: 20px;
  color: var(--color-text);
}

/* 表单组 */
.form-group {
  margin-bottom: 16px;
}
.form-group label {
  display: block;
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
}
.form-group input,
.form-group select {
  width: 100%;
  padding: 10px 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: var(--color-text);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}
.form-group input:focus,
.form-group select:focus {
  border-color: var(--color-primary);
}
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.required {
  color: var(--color-danger);
}
.optional {
  color: var(--color-text-secondary);
  font-size: 12px;
}

/* 弹窗按钮 */
.dialog-buttons {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 24px;
}
.btn-cancel {
  padding: 10px 24px;
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: var(--color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-cancel:hover {
  color: var(--color-text);
  border-color: var(--color-text-secondary);
}
.btn-submit {
  padding: 10px 24px;
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  border: none;
  border-radius: var(--radius);
  color: var(--color-bg);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn-submit:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}
.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
.btn-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(10, 14, 23, 0.3);
  border-top-color: var(--color-bg);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 640px) {
  .edu-card__body {
    grid-template-columns: 1fr;
  }
  .dialog-card {
    padding: 24px 20px;
  }
  .form-row {
    grid-template-columns: 1fr;
  }
  .dialog-buttons {
    flex-direction: column-reverse;
  }
  .btn-cancel,
  .btn-submit {
    width: 100%;
    text-align: center;
    justify-content: center;
  }
}

/* ===== 移动端触摸优化 ===== */
@media (max-width: 767px) {
  .education-page {
    padding: 24px var(--container-px) 60px;
  }
  .page-title {
    font-size: 22px;
  }

  /* 页头按钮区 */
  .page-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  .btn-add {
    width: 100%;
    text-align: center;
    min-height: var(--touch-min);
  }

  /* 卡片body单列 */
  .edu-card__body {
    grid-template-columns: 1fr;
  }

  /* 弹窗底部滑入 */
  .dialog-overlay {
    align-items: flex-end;
    padding: 0;
  }
  .dialog-card {
    border-radius: 16px 16px 0 0;
    max-width: 100%;
    max-height: 85vh;
    padding: 24px 20px;
  }

  /* 弹窗表单 */
  .form-group input,
  .form-group select {
    min-height: var(--input-min-h);
    font-size: 16px;
  }
  .form-row {
    grid-template-columns: 1fr;
  }

  /* 弹窗按钮 */
  .dialog-buttons {
    flex-direction: column-reverse;
  }
  .btn-cancel,
  .btn-submit {
    width: 100%;
    min-height: var(--touch-min);
    text-align: center;
    justify-content: center;
  }
}
</style>
