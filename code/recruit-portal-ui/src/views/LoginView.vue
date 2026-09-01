<template>
  <div class="login-page">
    <!-- ==================== 登录模式 ==================== -->
    <div class="form-card" v-if="!isResetMode">
      <h2 class="form-title">登录</h2>
      <p class="form-sub">登录遨天科技校园招聘系统</p>

      <!-- 错误提示 -->
      <div class="form-toast form-toast--error" v-if="error">{{ error }}</div>

      <form @submit.prevent="handleLogin">
        <!-- 手机号 -->
        <div class="form-group" :class="{ 'form-group--error': errors.phone }">
          <label>手机号</label>
          <input
            v-model="form.phone"
            type="tel"
            placeholder="请输入11位手机号"
            maxlength="11"
            @input="errors.phone = ''"
          />
          <span class="form-hint" v-if="errors.phone">{{ errors.phone }}</span>
        </div>

        <!-- 密码 -->
        <div class="form-group" :class="{ 'form-group--error': errors.password }">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            @input="errors.password = ''"
          />
          <span class="form-hint" v-if="errors.password">{{ errors.password }}</span>
        </div>

        <!-- 忘记密码 -->
        <div class="form-extra">
          <a href="#" class="form-link" @click.prevent="switchToReset">忘记密码？</a>
        </div>

        <!-- 登录按钮 -->
        <button type="submit" class="btn-submit" :disabled="loading">
          <span class="btn-spinner" v-if="loading"></span>
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <!-- 底部链接 -->
      <p class="form-foot">
        没有账号？<router-link to="/register">立即注册</router-link>
      </p>
    </div>

    <!-- ==================== 重置密码模式 ==================== -->
    <div class="form-card" v-else>
      <h2 class="form-title">重置密码</h2>
      <p class="form-sub">通过短信验证码重置您的登录密码</p>

      <!-- 成功 / 错误提示 -->
      <div class="form-toast form-toast--error" v-if="error">{{ error }}</div>
      <div class="form-toast form-toast--success" v-if="success">{{ success }}</div>

      <!-- 步骤1：获取验证码并重置 -->
      <form @submit.prevent="handleReset" v-if="!resetDone">
        <!-- 手机号 -->
        <div class="form-group" :class="{ 'form-group--error': resetErrors.phone }">
          <label>手机号</label>
          <input
            v-model="resetForm.phone"
            type="tel"
            placeholder="请输入11位手机号"
            maxlength="11"
            @input="resetErrors.phone = ''"
          />
          <span class="form-hint" v-if="resetErrors.phone">{{ resetErrors.phone }}</span>
        </div>

        <!-- 图形验证码 -->
        <div class="form-group" :class="{ 'form-group--error': resetErrors.captchaCode }">
          <label>图形验证码</label>
          <div class="captcha-row">
            <input
              v-model="resetForm.captchaCode"
              type="text"
              placeholder="请输入验证码"
              maxlength="6"
              class="captcha-input"
              @input="resetErrors.captchaCode = ''"
            />
            <img
              :src="captchaImage"
              alt="验证码"
              class="captcha-img"
              @click="refreshCaptcha"
              title="点击刷新验证码"
            />
          </div>
          <span class="form-hint" v-if="resetErrors.captchaCode">{{ resetErrors.captchaCode }}</span>
        </div>

        <!-- 短信验证码 -->
        <div class="form-group" :class="{ 'form-group--error': resetErrors.smsCode }">
          <label>短信验证码</label>
          <div class="sms-row">
            <input
              v-model="resetForm.smsCode"
              type="text"
              placeholder="请输入短信验证码"
              maxlength="6"
              class="sms-input"
              @input="resetErrors.smsCode = ''"
            />
            <button
              type="button"
              class="btn-sms"
              :disabled="smsCountdown > 0 || sendingSms"
              @click="sendResetSmsCode"
            >
              {{ smsCountdown > 0 ? `${smsCountdown}秒后重发` : sendingSms ? '发送中...' : '发送验证码' }}
            </button>
          </div>
          <span class="form-hint" v-if="resetErrors.smsCode">{{ resetErrors.smsCode }}</span>
        </div>

        <!-- 新密码 -->
        <div class="form-group" :class="{ 'form-group--error': resetErrors.newPassword }">
          <label>新密码</label>
          <input
            v-model="resetForm.newPassword"
            type="password"
            placeholder="至少6位新密码"
            @input="resetErrors.newPassword = ''"
          />
          <span class="form-hint" v-if="resetErrors.newPassword">{{ resetErrors.newPassword }}</span>
        </div>

        <!-- 确认新密码 -->
        <div class="form-group" :class="{ 'form-group--error': resetErrors.confirmPassword }">
          <label>确认新密码</label>
          <input
            v-model="resetForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            @input="resetErrors.confirmPassword = ''"
          />
          <span class="form-hint" v-if="resetErrors.confirmPassword">{{ resetErrors.confirmPassword }}</span>
        </div>

        <!-- 提交 -->
        <button type="submit" class="btn-submit" :disabled="resetting">
          <span class="btn-spinner" v-if="resetting"></span>
          {{ resetting ? '重置中...' : '重置密码' }}
        </button>
      </form>

      <!-- 步骤2：重置成功 -->
      <div class="reset-done" v-else>
        <svg class="reset-done__icon" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
          <polyline points="22 4 12 14.01 9 11.01"/>
        </svg>
        <h3>密码重置成功</h3>
        <p>请使用新密码登录</p>
        <button class="btn-submit" @click="switchToLogin">返回登录</button>
      </div>

      <!-- 底部链接 -->
      <p class="form-foot">
        <a href="#" class="form-link" @click.prevent="switchToLogin">返回登录</a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth.js';
import api from '../utils/axios.js';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

// ==================== 登录表单 ====================
const form = reactive({ phone: '', password: '' });
const errors = reactive({ phone: '', password: '' });
const loading = ref(false);
const error = ref('');

/** 是否处于重置密码模式 */
const isResetMode = ref(false);

/** 表单校验 */
function validate() {
  let valid = true;
  errors.phone = '';
  errors.password = '';

  if (!form.phone) {
    errors.phone = '请输入手机号';
    valid = false;
  } else if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errors.phone = '请输入正确的11位手机号';
    valid = false;
  }

  if (!form.password) {
    errors.password = '请输入密码';
    valid = false;
  }

  return valid;
}

/** 登录 */
async function handleLogin() {
  error.value = '';
  if (!validate()) return;

  loading.value = true;
  try {
    const res = await api.post('/auth/login', {
      phone: form.phone,
      password: form.password,
    });
    auth.setTokens(res.data.accessToken, res.data.refreshToken);
    if (res.data.user) auth.setUser(res.data.user);
    const redirect = route.query.redirect || '/';
    router.push(redirect);
  } catch (e) {
    // 业务错误（拦截器已 toast）：这里仅保留登录失败后的内联错误提示
    const data = e.response?.data;
    const msg = data?.msg;
    if (data?.code === 20005) {
      error.value = '账号已被锁定，请15分钟后再试';
    } else {
      error.value = msg || '登录失败，请稍后重试';
    }
  } finally {
    loading.value = false;
  }
}

// ==================== 重置密码 ====================
const resetForm = reactive({
  phone: '',
  captchaCode: '',
  smsCode: '',
  newPassword: '',
  confirmPassword: '',
});
const resetErrors = reactive({
  phone: '',
  captchaCode: '',
  smsCode: '',
  newPassword: '',
  confirmPassword: '',
});
const captchaKey = ref('');
const captchaImage = ref('');
const smsCountdown = ref(0);
const sendingSms = ref(false);
const resetting = ref(false);
const resetDone = ref(false);
const success = ref('');

let countdownTimer = null;

/** 切换到重置密码模式 */
function switchToReset() {
  isResetMode.value = true;
  error.value = '';
  success.value = '';
  resetDone.value = false;
  refreshCaptcha();
}

/** 切换到登录模式 */
function switchToLogin() {
  isResetMode.value = false;
  error.value = '';
  success.value = '';
  resetDone.value = false;
}

/** 刷新图形验证码 */
async function refreshCaptcha() {
  try {
    const res = await api.get('/auth/captcha');
    captchaKey.value = res.data.captchaKey;
    captchaImage.value = res.data.captchaImage;
    resetForm.captchaCode = '';
  } catch (e) {
    error.value = '获取验证码失败，请稍后重试';
  }
}

/** 发送短信验证码 */
async function sendResetSmsCode() {
  if (!resetForm.phone) {
    resetErrors.phone = '请先输入手机号';
    return;
  }
  if (!/^1[3-9]\d{9}$/.test(resetForm.phone)) {
    resetErrors.phone = '请输入正确的11位手机号';
    return;
  }
  if (!resetForm.captchaCode) {
    resetErrors.captchaCode = '请先输入图形验证码';
    return;
  }

  error.value = '';
  sendingSms.value = true;
  try {
    await api.post('/auth/sms-code', {
      phone: resetForm.phone,
      captchaKey: captchaKey.value,
      captchaCode: resetForm.captchaCode,
    });
    smsCountdown.value = 60;
    countdownTimer = setInterval(() => {
      smsCountdown.value--;
      if (smsCountdown.value <= 0) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }, 1000);
    success.value = '验证码已发送，请注意查收';
  } catch (e) {
    const msg = e.response?.data?.msg || '发送验证码失败，请重试';
    error.value = msg;
    // 图形验证码错误时自动刷新
    if (e.response?.data?.code === 20004) {
      refreshCaptcha();
    }
  } finally {
    sendingSms.value = false;
  }
}

/** 重置密码校验 */
function validateReset() {
  let valid = true;
  Object.keys(resetErrors).forEach(k => (resetErrors[k] = ''));

  if (!resetForm.phone) {
    resetErrors.phone = '请输入手机号';
    valid = false;
  } else if (!/^1[3-9]\d{9}$/.test(resetForm.phone)) {
    resetErrors.phone = '请输入正确的11位手机号';
    valid = false;
  }
  if (!resetForm.captchaCode) {
    resetErrors.captchaCode = '请输入图形验证码';
    valid = false;
  }
  if (!resetForm.smsCode) {
    resetErrors.smsCode = '请输入短信验证码';
    valid = false;
  }
  if (!resetForm.newPassword) {
    resetErrors.newPassword = '请输入新密码';
    valid = false;
  } else if (resetForm.newPassword.length < 6) {
    resetErrors.newPassword = '密码至少6位';
    valid = false;
  }
  if (!resetForm.confirmPassword) {
    resetErrors.confirmPassword = '请再次输入新密码';
    valid = false;
  } else if (resetForm.newPassword !== resetForm.confirmPassword) {
    resetErrors.confirmPassword = '两次密码不一致';
    valid = false;
  }
  return valid;
}

/** 执行密码重置 */
async function handleReset() {
  error.value = '';
  success.value = '';
  if (!validateReset()) return;

  resetting.value = true;
  try {
    await api.post('/auth/reset-password', {
      phone: resetForm.phone,
      smsCode: resetForm.smsCode,
      newPassword: resetForm.newPassword,
    });
    resetDone.value = true;
  } catch (e) {
    error.value = e.response?.data?.msg || '重置失败，请稍后重试';
  } finally {
    resetting.value = false;
  }
}

/** 检测 URL 参数自动切换模式 */
onMounted(() => {
  if (route.query.mode === 'reset') {
    switchToReset();
  }
});

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
});
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 40px 24px;
}

.form-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 40px;
  width: 100%;
  max-width: 440px;
}

.form-title {
  font-size: 24px;
  margin-bottom: 4px;
  color: var(--color-text);
}

.form-sub {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

/* Toast 错误提示 */
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

/* 忘记密码 */
.form-extra {
  text-align: right;
  margin-bottom: 20px;
}
.form-link {
  font-size: 13px;
  color: var(--color-primary);
  text-decoration: none;
}
.form-link:hover {
  text-decoration: underline;
}

/* 图形验证码行 */
.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.captcha-input {
  flex: 1;
  padding: 10px 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: var(--color-text);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}
.captcha-input:focus {
  border-color: var(--color-primary);
}
.form-group--error .captcha-input {
  border-color: var(--color-danger);
}
.captcha-img {
  height: 42px;
  width: 110px;
  border-radius: 6px;
  border: 1px solid var(--color-border);
  cursor: pointer;
  flex-shrink: 0;
  object-fit: contain;
  background: #fff;
}
.captcha-img:hover {
  border-color: var(--color-primary);
}

/* 短信验证码行 */
.sms-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.sms-input {
  flex: 1;
  padding: 10px 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  color: var(--color-text);
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}
.sms-input:focus {
  border-color: var(--color-primary);
}
.form-group--error .sms-input {
  border-color: var(--color-danger);
}
.btn-sms {
  flex-shrink: 0;
  padding: 10px 16px;
  background: transparent;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  color: var(--color-primary);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  min-width: 100px;
}
.btn-sms:hover:not(:disabled) {
  background: rgba(95, 184, 214, 0.1);
}
.btn-sms:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  border-color: var(--color-border);
  color: var(--color-text-secondary);
}

/* 重置成功 */
.reset-done {
  text-align: center;
  padding: 20px 0;
}
.reset-done__icon {
  color: var(--color-success);
  margin-bottom: 16px;
}
.reset-done h3 {
  font-size: 18px;
  color: var(--color-text);
  margin-bottom: 8px;
}
.reset-done p {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
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

/* 加载旋转 */
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

/* 底部链接 */
.form-foot {
  text-align: center;
  margin-top: 20px;
  font-size: 14px;
  color: var(--color-text-secondary);
}
.form-foot a {
  color: var(--color-primary);
  text-decoration: none;
}
.form-foot a:hover {
  text-decoration: underline;
}

/* 响应式 */
@media (max-width: 767px) {
  .form-card {
    padding: 24px 16px;
  }
  .form-group input {
    min-height: var(--input-min-h);
    font-size: 16px;
    padding: 12px 16px;
  }
  .form-group label {
    margin-bottom: 8px;
  }
  .btn-submit {
    min-height: var(--input-min-h);
    font-size: 16px;
  }
  .captcha-row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }
  .captcha-img {
    width: 100%;
    height: 48px;
    object-fit: contain;
  }
}
</style>
