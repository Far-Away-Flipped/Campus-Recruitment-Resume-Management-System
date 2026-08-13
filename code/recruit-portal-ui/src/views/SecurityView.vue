<template>
  <div class="security-page">
    <div class="page-header">
      <h1 class="page-title">账号安全</h1>
      <p class="page-sub">修改您的登录密码</p>
    </div>

    <!-- Toast 提示 -->
    <div class="form-toast form-toast--success" v-if="success">{{ success }}</div>
    <div class="form-toast form-toast--error" v-if="error">{{ error }}</div>

    <div class="security-card">
      <form @submit.prevent="handleSubmit">
        <!-- 原密码 -->
        <div class="form-group" :class="{ 'form-group--error': fieldErrors.oldPassword }">
          <label>原密码 <span class="required">*</span></label>
          <input
            v-model="form.oldPassword"
            type="password"
            placeholder="请输入原密码"
            @input="fieldErrors.oldPassword = ''"
          />
          <span class="form-hint" v-if="fieldErrors.oldPassword">{{ fieldErrors.oldPassword }}</span>
        </div>

        <!-- 新密码 -->
        <div class="form-group" :class="{ 'form-group--error': fieldErrors.newPassword }">
          <label>新密码 <span class="required">*</span></label>
          <input
            v-model="form.newPassword"
            type="password"
            placeholder="至少8位，需包含字母和数字"
            @input="fieldErrors.newPassword = ''"
          />
          <span class="form-hint" v-if="fieldErrors.newPassword">{{ fieldErrors.newPassword }}</span>
        </div>

        <!-- 确认新密码 -->
        <div class="form-group" :class="{ 'form-group--error': fieldErrors.confirmPassword }">
          <label>确认新密码 <span class="required">*</span></label>
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            @input="fieldErrors.confirmPassword = ''"
          />
          <span class="form-hint" v-if="fieldErrors.confirmPassword">{{ fieldErrors.confirmPassword }}</span>
        </div>

        <!-- 提交 -->
        <button type="submit" class="btn-submit" :disabled="saving">
          <span class="btn-spinner" v-if="saving"></span>
          {{ saving ? '提交中...' : '确认修改' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import api from '../utils/axios.js';

const router = useRouter();

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});
const fieldErrors = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});
const saving = ref(false);
const error = ref('');
const success = ref('');

/** 表单校验 */
function validate() {
  let valid = true;
  fieldErrors.oldPassword = '';
  fieldErrors.newPassword = '';
  fieldErrors.confirmPassword = '';

  if (!form.oldPassword) {
    fieldErrors.oldPassword = '请输入原密码';
    valid = false;
  }
  if (!form.newPassword) {
    fieldErrors.newPassword = '请输入新密码';
    valid = false;
  } else if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(form.newPassword)) {
    fieldErrors.newPassword = '新密码至少8位，且需同时包含字母和数字';
    valid = false;
  }
  if (!form.confirmPassword) {
    fieldErrors.confirmPassword = '请再次输入新密码';
    valid = false;
  } else if (form.newPassword !== form.confirmPassword) {
    fieldErrors.confirmPassword = '两次输入的密码不一致';
    valid = false;
  }
  return valid;
}

/** 提交修改密码 */
async function handleSubmit() {
  error.value = '';
  success.value = '';
  if (!validate()) return;

  saving.value = true;
  try {
    await api.post('/auth/change-password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    });
    success.value = '密码已修改，请重新登录';
    // 清除本地登录态并跳转登录页
    localStorage.clear();
    setTimeout(() => {
      router.push('/login');
    }, 1200);
  } catch (e) {
    error.value = e.response?.data?.msg || '修改失败，请稍后重试';
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.security-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 24px;
}

.page-header {
  margin-bottom: 24px;
}
.page-title {
  font-size: 28px;
  color: var(--color-text);
}
.page-sub {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-top: 6px;
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

/* 卡片 */
.security-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 32px;
  max-width: 520px;
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
.form-group input {
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
.form-group input:focus {
  border-color: var(--color-primary);
}
.form-group--error input {
  border-color: var(--color-danger);
}
.form-hint {
  display: block;
  font-size: 12px;
  color: var(--color-danger);
  margin-top: 4px;
}
.required {
  color: var(--color-danger);
}

/* 提交按钮 */
.btn-submit {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  border: none;
  border-radius: var(--radius);
  color: var(--color-bg);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
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
  width: 16px;
  height: 16px;
  border: 2px solid rgba(10, 14, 23, 0.3);
  border-top-color: var(--color-bg);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 767px) {
  .security-page {
    padding: 24px var(--container-px) 60px;
  }
  .page-title {
    font-size: 22px;
  }
  .security-card {
    padding: 24px 20px;
    max-width: 100%;
  }
  .form-group input {
    min-height: var(--input-min-h);
    font-size: 16px;
    padding: 12px 16px;
  }
  .btn-submit {
    min-height: var(--touch-min);
  }
}
</style>
