<template>
  <div class="profile-page">
    <el-card shadow="never" class="profile-card">
      <el-tabs v-model="activeTab" class="profile-tabs">
        <!-- 个人信息 -->
        <el-tab-pane label="个人信息" name="info">
          <el-form
            ref="infoFormRef"
            :model="infoForm"
            :rules="infoRules"
            label-width="90px"
            size="default"
            v-loading="infoLoading"
          >
            <el-form-item label="用户名">
              <el-input :model-value="infoForm.userName" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model="infoForm.nickName" placeholder="请输入昵称" maxlength="30" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="infoForm.email" placeholder="请输入邮箱" maxlength="100" />
            </el-form-item>
            <el-form-item label="手机号" prop="phonenumber">
              <el-input v-model="infoForm.phonenumber" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="性别" prop="sex">
              <el-radio-group v-model="infoForm.sex">
                <el-radio value="0">男</el-radio>
                <el-radio value="1">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="infoSaving" @click="handleSaveInfo">保存</el-button>
              <el-button @click="loadUserInfo">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="90px"
            size="default"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password maxlength="32" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" placeholder="至少8位，需包含字母和数字" show-password maxlength="32" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password maxlength="32" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdSaving" @click="handleChangePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import request from '@/utils/request';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const activeTab = ref('info');

// ===== 个人信息 =====
const infoLoading = ref(false);
const infoSaving = ref(false);
const infoFormRef = ref(null);

const infoForm = reactive({
  userName: '',
  nickName: '',
  email: '',
  phonenumber: '',
  sex: '0',
});

const infoRules = {
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
  phonenumber: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
};

async function loadUserInfo() {
  infoLoading.value = true;
  try {
    const res = await request.get('/auth/info');
    const info = res.data || {};
    infoForm.userName = info.userName || '';
    infoForm.nickName = info.nickName || '';
    infoForm.email = info.email || '';
    infoForm.phonenumber = info.phonenumber || '';
    infoForm.sex = info.sex ?? '0';
  } catch {
    // 错误已在拦截器提示
  } finally {
    infoLoading.value = false;
  }
}

async function handleSaveInfo() {
  const valid = await infoFormRef.value.validate().catch(() => false);
  if (!valid) return;

  infoSaving.value = true;
  try {
    await request.put('/auth/profile', {
      nickName: infoForm.nickName,
      email: infoForm.email,
      phonenumber: infoForm.phonenumber,
      sex: infoForm.sex,
    });
    ElMessage.success('个人信息已保存');
    // 同步刷新本地缓存的用户信息（失败静默，不影响保存结果）
    authStore.getUserInfo().catch(() => {});
  } catch {
    // 错误已在拦截器提示
  } finally {
    infoSaving.value = false;
  }
}

// ===== 修改密码 =====
const pwdSaving = ref(false);
const pwdFormRef = ref(null);

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

// 密码强度校验：至少8位且同时包含字母和数字
function validatePasswordStrength(rule, value, callback) {
  if (!value) {
    callback(new Error('请输入新密码'));
    return;
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(value)) {
    callback(new Error('新密码至少8位，且需同时包含字母和数字'));
    return;
  }
  callback();
}

function validateConfirmPassword(rule, value, callback) {
  if (!value) {
    callback(new Error('请再次输入新密码'));
    return;
  }
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'));
    return;
  }
  callback();
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ validator: validatePasswordStrength, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
};

async function handleChangePassword() {
  const valid = await pwdFormRef.value.validate().catch(() => false);
  if (!valid) return;

  pwdSaving.value = true;
  try {
    await request.put('/auth/password', {
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    });
    ElMessage.success('密码已修改，请重新登录');
    // 清除登录态并跳转登录页
    authStore.logout();
    router.push('/login');
  } catch {
    // 错误已在拦截器提示
  } finally {
    pwdSaving.value = false;
  }
}

onMounted(() => {
  loadUserInfo();
});
</script>

<style scoped>
.profile-page {
  max-width: 720px;
  margin: 0 auto;
}

.profile-card {
  margin-bottom: 16px;
}

.profile-tabs {
  padding: 8px 12px 12px;
}

.profile-tabs :deep(.el-tabs__content) {
  max-width: 560px;
  padding-top: 16px;
}
</style>
