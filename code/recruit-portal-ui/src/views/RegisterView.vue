<template>
  <div class="register-page">
    <div class="form-card">
      <h2 class="form-title">注册</h2>
      <p class="form-sub">创建遨天科技校园招聘账号</p>

      <!-- 错误 / 成功提示 -->
      <div class="form-toast form-toast--error" v-if="error">{{ error }}</div>
      <div class="form-toast form-toast--success" v-if="success">{{ success }}</div>

      <form @submit.prevent="handleRegister">
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

        <!-- 图形验证码 -->
        <div class="form-group" :class="{ 'form-group--error': errors.captchaCode }">
          <label>图形验证码</label>
          <div class="captcha-row">
            <input
              v-model="form.captchaCode"
              type="text"
              placeholder="请输入验证码"
              maxlength="6"
              class="captcha-input"
              @input="errors.captchaCode = ''"
            />
            <img
              :src="captchaImage"
              alt="验证码"
              class="captcha-img"
              @click="refreshCaptcha"
              title="点击刷新验证码"
            />
          </div>
          <span class="form-hint" v-if="errors.captchaCode">{{ errors.captchaCode }}</span>
        </div>

        <!-- 短信验证码 -->
        <div class="form-group" :class="{ 'form-group--error': errors.smsCode }">
          <label>短信验证码</label>
          <div class="sms-row">
            <input
              v-model="form.smsCode"
              type="text"
              placeholder="请输入短信验证码"
              maxlength="6"
              class="sms-input"
              @input="errors.smsCode = ''"
            />
            <button
              type="button"
              class="btn-sms"
              :disabled="smsCountdown > 0 || sendingSms"
              @click="sendSmsCode"
            >
              {{ smsCountdown > 0 ? `${smsCountdown}秒后重发` : sendingSms ? '发送中...' : '发送验证码' }}
            </button>
          </div>
          <span class="form-hint" v-if="errors.smsCode">{{ errors.smsCode }}</span>
          <span class="form-hint form-hint--code" v-if="smsCodeHint">
            📱 验证码：<strong>{{ smsCodeHint }}</strong>（已自动填入）
          </span>
        </div>

        <!-- 密码 -->
        <div class="form-group" :class="{ 'form-group--error': errors.password }">
          <label>密码</label>
          <input
            v-model="form.password"
            type="password"
            placeholder="至少6位密码"
            @input="errors.password = ''"
          />
          <span class="form-hint" v-if="errors.password">{{ errors.password }}</span>
        </div>

        <!-- 确认密码 -->
        <div class="form-group" :class="{ 'form-group--error': errors.confirmPassword }">
          <label>确认密码</label>
          <input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            @input="errors.confirmPassword = ''"
          />
          <span class="form-hint" v-if="errors.confirmPassword">{{ errors.confirmPassword }}</span>
        </div>

        <!-- 隐私政策勾选 -->
        <div class="form-group form-group--checkbox" :class="{ 'form-group--error': errors.privacy }">
          <label class="checkbox-label">
            <input type="checkbox" v-model="form.agreePrivacy" @change="errors.privacy = ''" />
            <span class="checkbox-text">
              我已阅读并同意<router-link to="/privacy" target="_blank" class="form-link">《隐私政策》</router-link>
            </span>
          </label>
          <span class="form-hint" v-if="errors.privacy">{{ errors.privacy }}</span>
        </div>

        <!-- 注册按钮 -->
        <button type="submit" class="btn-submit" :disabled="loading">
          <span class="btn-spinner" v-if="loading"></span>
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>

      <!-- 底部链接 -->
      <p class="form-foot">
        已有账号？<router-link to="/login">去登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth.js';
import api from '../utils/axios.js';

const router = useRouter();
const auth = useAuthStore();

const form = reactive({
  phone: '',
  captchaCode: '',
  smsCode: '',
  password: '',
  confirmPassword: '',
  agreePrivacy: false,
});

const errors = reactive({
  phone: '',
  captchaCode: '',
  smsCode: '',
  password: '',
  confirmPassword: '',
  privacy: '',
});

const captchaKey = ref('');
const captchaImage = ref('');
const smsCountdown = ref(0);
const sendingSms = ref(false);
const smsCodeHint = ref('');
const loading = ref(false);
const error = ref('');
const success = ref('');

let countdownTimer = null;

/** 刷新图形验证码 */
async function refreshCaptcha() {
  try {
    const res = await api.get('/auth/captcha');
    // res = { code: 200, data: { captchaKey, captchaImage } }
    captchaKey.value = res.data.captchaKey;
    captchaImage.value = res.data.captchaImage;
    form.captchaCode = '';
  } catch (e) {
    error.value = '获取验证码失败，请稍后重试';
  }
}

/** 发送短信验证码 */
async function sendSmsCode() {
  // 前置校验
  if (!form.phone) {
    errors.phone = '请先输入手机号';
    return;
  }
  if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errors.phone = '请输入正确的11位手机号';
    return;
  }
  if (!form.captchaCode) {
    errors.captchaCode = '请先输入图形验证码';
    return;
  }

  error.value = '';
  sendingSms.value = true;
  try {
    const res = await api.post('/auth/sms-code', {
      phone: form.phone,
      captchaKey: captchaKey.value,
      captchaCode: form.captchaCode,
    });
    // 开发环境：API直接返回验证码，展示在页面上
    if (res.data?.code) {
      smsCodeHint.value = res.data.code;
      form.smsCode = res.data.code; // 自动填入
    }
    // 启动60秒倒计时
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
    const msg = e.response?.data?.message || '发送验证码失败，请重试';
    error.value = msg;
    // 验证码错误时自动刷新
    if (e.response?.data?.code === 20003) {
      refreshCaptcha();
    }
  } finally {
    sendingSms.value = false;
  }
}

/** 表单校验 */
function validate() {
  let valid = true;
  // 清空错误
  Object.keys(errors).forEach(k => (errors[k] = ''));

  if (!form.phone) {
    errors.phone = '请输入手机号';
    valid = false;
  } else if (!/^1[3-9]\d{9}$/.test(form.phone)) {
    errors.phone = '请输入正确的11位手机号';
    valid = false;
  }

  if (!form.captchaCode) {
    errors.captchaCode = '请输入图形验证码';
    valid = false;
  }

  if (!form.smsCode) {
    errors.smsCode = '请输入短信验证码';
    valid = false;
  }

  if (!form.password) {
    errors.password = '请输入密码';
    valid = false;
  } else if (form.password.length < 6) {
    errors.password = '密码至少6位';
    valid = false;
  }

  if (!form.confirmPassword) {
    errors.confirmPassword = '请再次输入密码';
    valid = false;
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = '两次密码不一致';
    valid = false;
  }

  if (!form.agreePrivacy) {
    errors.privacy = '请阅读并同意隐私政策';
    valid = false;
  }

  return valid;
}

/** 注册 */
async function handleRegister() {
  error.value = '';
  success.value = '';
  if (!validate()) return;

  loading.value = true;
  try {
    const res = await api.post('/auth/register', {
      phone: form.phone,
      smsCode: form.smsCode,
      password: form.password,
      privacyAgreed: true,
    });
    // 注册成功 —— res 已被 axios 拦截器解包为业务对象 {code,msg,data}
    if (res.code === 200 && res.data) {
      auth.setTokens(res.data.accessToken, res.data.refreshToken);
      router.push('/');
    } else {
      error.value = res.msg || '注册失败，请稍后重试';
    }
  } catch (e) {
    const code = e.response?.data?.code;
    const msg = e.response?.data?.message;
    if (code === 20004) {
      error.value = '手机号已注册，请直接登录';
    } else {
      error.value = msg || '注册失败，请稍后重试';
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  refreshCaptcha();
});

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
    countdownTimer = null;
  }
});
</script>

<style scoped>
.register-page {
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

/* Toast 提示 */
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
.form-group input[type="text"],
.form-group input[type="tel"],
.form-group input[type="password"] {
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

/* 隐私勾选 */
.form-group--checkbox {
  margin-bottom: 20px;
}
.checkbox-label {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
}
.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  margin-top: 2px;
  accent-color: var(--color-primary);
  flex-shrink: 0;
}
.checkbox-text {
  color: var(--color-text-secondary);
  line-height: 1.5;
}
.checkbox-text .form-link {
  color: var(--color-primary);
  text-decoration: none;
}
.checkbox-text .form-link:hover {
  text-decoration: underline;
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
  .form-group input[type="text"],
  .form-group input[type="tel"],
  .form-group input[type="password"] {
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
  .checkbox-label {
    min-height: var(--touch-min);
    padding: 8px 0;
    align-items: center;
  }
  .checkbox-label input[type="checkbox"] {
    width: 20px;
    height: 20px;
  }
}
</style>
