<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="brand-logo">
            <el-icon :size="48"><Monitor /></el-icon>
          </div>
          <h1 class="brand-title">遨天校园招聘</h1>
          <p class="brand-desc">智慧招聘管理系统</p>
          <p class="brand-sub">高效、精准、智能的校园招聘解决方案</p>
        </div>
        <div class="brand-pattern"></div>
      </div>

      <!-- 右侧表单区 -->
      <div class="login-form-area">
        <div class="form-card">
          <h2 class="form-title">管理员登录</h2>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            size="large"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
              />
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
              />
            </el-form-item>

            <el-form-item prop="captchaCode">
              <div class="captcha-row">
                <el-input
                  v-model="form.captchaCode"
                  placeholder="验证码"
                  maxlength="4"
                  :prefix-icon="Key"
                />
                <img
                  :src="captchaSrc"
                  class="captcha-img"
                  alt="验证码"
                  title="点击刷新"
                  @click="refreshCaptcha"
                />
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                class="login-btn"
                :loading="loading"
                @click="handleLogin"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <p class="login-footer">Copyright &copy; {{ year }} 遨天科技 All Rights Reserved.</p>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, Key } from '@element-plus/icons-vue';
import request from '@/utils/request';

const router = useRouter();
const formRef = ref(null);
const loading = ref(false);
const captchaKey = ref(Date.now().toString());

const year = computed(() => new Date().getFullYear());

const captchaSrc = computed(() => `/api/admin/auth/captcha?key=${captchaKey.value}`);

const form = reactive({
  username: '',
  password: '',
  captchaCode: '',
});

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
};

function refreshCaptcha() {
  captchaKey.value = Date.now().toString();
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  loading.value = true;
  try {
    const res = await request.post('/auth/login', {
      username: form.username,
      password: form.password,
      captchaCode: form.captchaCode,
      captchaKey: captchaKey.value,
    });
    const { token } = res.data;
    localStorage.setItem('admin_token', token);
    ElMessage.success('登录成功');
    router.push('/dashboard');
  } catch {
    refreshCaptcha();
    form.captchaCode = '';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #e8f4f8 0%, #f5f7fa 50%, #e0f0f5 100%);
}

.login-container {
  display: flex;
  width: 880px;
  min-height: 520px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
}

/* 左侧品牌区 */
.login-brand {
  position: relative;
  flex: 1;
  background: linear-gradient(160deg, #5FB8D6 0%, #3d8fa8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.brand-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
  padding: 40px;
}

.brand-logo {
  margin-bottom: 20px;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 2px;
  margin-bottom: 12px;
}

.brand-desc {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.brand-sub {
  font-size: 13px;
  opacity: 0.7;
}

.brand-pattern {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 80%, rgba(255,255,255,0.1) 0%, transparent 60%),
              radial-gradient(circle at 80% 20%, rgba(255,255,255,0.08) 0%, transparent 60%);
}

/* 右侧表单区 */
.login-form-area {
  flex: 1;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.form-card {
  width: 100%;
  max-width: 340px;
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  margin-bottom: 32px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img {
  width: 110px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  flex-shrink: 0;
}

.captcha-img:hover {
  border-color: #5FB8D6;
}

.login-footer {
  margin-top: 24px;
  font-size: 12px;
  color: #999;
}

/* 响应式 */
@media (max-width: 920px) {
  .login-container {
    flex-direction: column;
    width: 92vw;
    min-height: auto;
  }

  .login-brand {
    min-height: 180px;
  }

  .brand-title {
    font-size: 22px;
  }

  .login-form-area {
    padding: 24px;
  }
}
</style>
