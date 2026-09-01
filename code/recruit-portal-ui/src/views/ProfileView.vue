<template>
  <div class="profile-page" v-motion-fade="{ y: 24 }">
    <h1 class="page-title">个人中心</h1>

    <!-- ===== 持久化 Tab 导航栏 ===== -->
    <div class="tabs-scroll">
      <div class="tabs" ref="tabsRef">
        <a
          class="tab"
          :class="{ 'tab--active': isBasicOrPrivacyTab && !localPrivacyActive }"
          data-tab="home"
          @click.prevent="switchToBasic"
          href="#"
        >
          基本资料
        </a>
        <router-link
          to="/profile/education"
          class="tab"
          :class="{ 'tab--active': routeName === 'education' }"
        >
          教育经历
        </router-link>
        <router-link
          to="/profile/resume"
          class="tab"
          :class="{ 'tab--active': routeName === 'resume' }"
        >
          简历附件
        </router-link>
        <router-link
          to="/profile/internship"
          class="tab"
          :class="{ 'tab--active': routeName === 'internship' }"
        >
          实习/项目
        </router-link>
        <router-link
          to="/profile/certificate"
          class="tab"
          :class="{ 'tab--active': routeName === 'certificate' }"
        >
          技能证书
        </router-link>
        <router-link
          to="/profile/activity"
          class="tab"
          :class="{ 'tab--active': routeName === 'activity' }"
        >
          社团经历
        </router-link>
        <a
          class="tab"
          :class="{ 'tab--active': localPrivacyActive }"
          data-tab="privacy"
          @click.prevent="switchToPrivacy"
          href="#"
        >
          隐私设置
        </a>
        <router-link
          to="/profile/security"
          class="tab"
          :class="{ 'tab--active': routeName === 'security' }"
        >
          账号安全
        </router-link>

        <!-- Active 指示器 -->
        <div
          class="tab-indicator"
          :style="{ left: indicatorLeft + 'px', width: indicatorWidth + 'px' }"
        ></div>
      </div>
    </div>

    <!-- 加载中 -->
    <LoadingSpinner :visible="loading" text="加载个人信息..." />

    <!-- 基本资料（路由为 /profile 且未激活隐私设置） -->
    <template v-if="!loading && showBasicInfo">
      <!-- Toast 提示 -->
      <div class="form-toast form-toast--success" v-if="success">{{ success }}</div>
      <div class="form-toast form-toast--error" v-if="error">{{ error }}</div>

      <div class="profile-card">
        <!-- 头像 -->
        <div class="avatar-section">
          <div class="avatar-preview" @click="triggerAvatar">
            <img v-if="avatarPreview" :src="avatarPreview" alt="头像" class="avatar-img" />
            <span v-else class="avatar-placeholder">
              {{ (form.name || '用')[0] }}
            </span>
          </div>
          <div class="avatar-info">
            <p class="avatar-title">个人头像</p>
            <p class="avatar-hint">支持 JPG、PNG、WebP 格式，大小不超过 2MB</p>
          </div>
          <input
            ref="avatarInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            class="avatar-file-input"
            @change="handleAvatarChange"
          />
        </div>

        <form @submit.prevent="handleSave">
          <!-- 姓名 -->
          <div class="form-row">
            <div class="form-group">
              <label>姓名</label>
              <input v-model="form.name" type="text" placeholder="请输入真实姓名" />
            </div>
            <!-- 性别 -->
            <div class="form-group">
              <label>性别</label>
              <div class="radio-group">
                <label class="radio-label">
                  <input type="radio" v-model="form.gender" value="M" />
                  <span>男</span>
                </label>
                <label class="radio-label">
                  <input type="radio" v-model="form.gender" value="F" />
                  <span>女</span>
                </label>
              </div>
            </div>
          </div>

          <!-- 出生年月 + 手机号 -->
          <div class="form-row">
            <div class="form-group">
              <label>出生年月</label>
              <input v-model="form.birthDate" type="date" />
            </div>
            <div class="form-group">
              <label>手机号</label>
              <input :value="form.phone" type="tel" readonly class="input-readonly" />
            </div>
          </div>

          <!-- 邮箱 -->
          <div class="form-group">
            <label>邮箱</label>
            <input v-model="form.email" type="email" placeholder="请输入邮箱地址" />
          </div>

          <!-- 现居地 + 户籍地 -->
          <div class="form-row">
            <div class="form-group">
              <label>现居地</label>
              <input v-model="form.currentCity" type="text" placeholder="例如：北京市海淀区" />
            </div>
            <div class="form-group">
              <label>户籍地（选填）</label>
              <input v-model="form.nativePlace" type="text" placeholder="例如：江苏省南京市" />
            </div>
          </div>

          <!-- 保存按钮 -->
          <button type="submit" class="btn-submit" :disabled="saving">
            <span class="btn-spinner" v-if="saving"></span>
            {{ saving ? '保存中...' : '保存修改' }}
          </button>
        </form>
      </div>
    </template>

    <!-- 隐私设置（本地状态激活） -->
    <template v-if="!loading && localPrivacyActive">
      <div class="profile-card">
        <h3 class="section-title">隐私设置</h3>
        <p class="section-desc">管理您的个人信息可见性和通知偏好。</p>
        <div class="privacy-list">
          <div class="privacy-item">
            <div class="privacy-item-text">
              <p class="privacy-item-title">简历对已投递企业可见</p>
              <p class="privacy-item-desc">您投递岗位的企业 HR 可以查看您的完整简历</p>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" checked disabled />
              <span class="toggle-slider"></span>
            </label>
          </div>
          <div class="privacy-item">
            <div class="privacy-item-text">
              <p class="privacy-item-title">消息通知</p>
              <p class="privacy-item-desc">接收投递进度、面试邀请等通知</p>
            </div>
            <label class="toggle-switch">
              <input type="checkbox" checked />
              <span class="toggle-slider"></span>
            </label>
          </div>
        </div>
      </div>

      <!-- 个人信息权利中心 -->
      <div class="profile-card" style="margin-top: 20px;">
        <h3 class="section-title">个人信息权利</h3>
        <p class="section-desc">根据《个人信息保护法》，您对自己的个人信息享有查阅、复制、更正、删除、撤回同意、注销账号等权利。</p>

        <!-- Toast 提示 -->
        <div class="form-toast form-toast--success" v-if="privacySuccess">{{ privacySuccess }}</div>
        <div class="form-toast form-toast--error" v-if="privacyError">{{ privacyError }}</div>

        <div class="rights-list">
          <!-- 导出我的数据 -->
          <div class="rights-item">
            <div class="rights-item__text">
              <p class="rights-item__title">导出我的数据</p>
              <p class="rights-item__desc">下载您在本系统中的所有个人信息副本</p>
            </div>
            <button
              class="btn-rights"
              :disabled="exporting"
              @click="handleExportData"
            >
              <span class="btn-spinner" v-if="exporting"></span>
              {{ exporting ? '导出中...' : '导出数据' }}
            </button>
          </div>

          <!-- 撤回同意 -->
          <div class="rights-item">
            <div class="rights-item__text">
              <p class="rights-item__title">撤回同意</p>
              <p class="rights-item__desc">撤回对个人信息收集和使用的同意</p>
            </div>
            <button
              class="btn-rights btn-rights--warning"
              :disabled="withdrawing"
              @click="handleWithdrawConsent"
            >
              {{ withdrawing ? '处理中...' : '撤回同意' }}
            </button>
          </div>

          <!-- 注销账号 -->
          <div class="rights-item">
            <div class="rights-item__text">
              <p class="rights-item__title">注销账号</p>
              <p class="rights-item__desc">永久删除您的账号及所有关联数据，此操作不可撤销</p>
            </div>
            <button
              class="btn-rights btn-rights--danger"
              :disabled="deletingAccount"
              @click="handleDeleteAccount"
            >
              {{ deletingAccount ? '处理中...' : '注销账号' }}
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- ===== 子路由渲染 ===== -->
    <router-view v-if="showChildRoute" v-slot="{ Component: ChildComp, route: childRoute }">
      <transition name="fade" mode="out-in">
        <component :is="ChildComp" :key="childRoute.path" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '../utils/axios.js';
import LoadingSpinner from '../components/LoadingSpinner.vue';

const route = useRoute();
const router = useRouter();

const tabsRef = ref(null);
const loading = ref(true);
const saving = ref(false);
const error = ref('');
const success = ref('');

// 隐私设置本地状态（不改变 URL）
const localPrivacyActive = ref(false);

// 路由名称简写
const routeName = computed(() => route.name);

// 是否展示基本资料（路由为 /profile 且未激活隐私设置）
const showBasicInfo = computed(() => {
  return !localPrivacyActive.value &&
    (!routeName.value || routeName.value === 'profile' || routeName.value === 'profile-home');
});

// 是否展示子路由
const showChildRoute = computed(() => {
  return !localPrivacyActive.value &&
    routeName.value && routeName.value !== 'profile' && routeName.value !== 'profile-home';
});

// 基本资料/隐私设置 Tab 是否 active
const isBasicOrPrivacyTab = computed(() => {
  return routeName.value === 'profile' || routeName.value === 'profile-home' || !routeName.value;
});

// 指示器位置
const indicatorLeft = ref(0);
const indicatorWidth = ref(0);

function switchToPrivacy() {
  localPrivacyActive.value = true;
  // 确保 URL 回到 /profile
  if (route.path !== '/profile') {
    router.replace('/profile');
  }
  nextTick(updateIndicator);
}

function switchToBasic() {
  localPrivacyActive.value = false;
  // 确保 URL 回到 /profile
  if (route.path !== '/profile') {
    router.replace('/profile');
  }
  nextTick(updateIndicator);
}

/** 更新指示器位置 + 自动滚动到可见 */
function updateIndicator() {
  if (!tabsRef.value) return;
  const selector = localPrivacyActive.value
    ? '[data-tab="privacy"]'
    : routeName.value === 'profile' || routeName.value === 'profile-home' || !routeName.value
      ? '[data-tab="home"]'
      : `.tab--active`;
  const activeEl = tabsRef.value.querySelector(selector);
  if (!activeEl) return;

  // offsetLeft 相对于 offsetParent，需补偿横向滚动偏移（移动端）
  const scrollContainer = tabsRef.value.parentElement; // .tabs-scroll
  const scrollOffset = scrollContainer ? scrollContainer.scrollLeft : 0;
  indicatorLeft.value = activeEl.offsetLeft - scrollOffset;
  indicatorWidth.value = activeEl.offsetWidth;

  // 移动端自动滚动到视野中央
  if (window.innerWidth <= 767) {
    activeEl.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
  }
}

// 路由变化 → 退出隐私模式 + 更新指示器 + 滚动到顶部 + 刷新数据
watch(() => route.path, (newPath) => {
  if (newPath !== '/profile') {
    localPrivacyActive.value = false;
  } else {
    // 回到基本资料 Tab 时重新加载个人信息（可能在子 Tab 中修改了关联数据）
    loadProfile();
  }
  // Tab 切换时滚动到页面顶部，避免停留在上一页的滚动位置
  window.scrollTo({ top: 0, behavior: 'instant' });
  nextTick(updateIndicator);
});

// 监听隐私设置状态 → 更新指示器
watch(localPrivacyActive, () => {
  nextTick(updateIndicator);
});

const avatarInput = ref(null);
const avatarPreview = ref(null);
const avatarFile = ref(null);

// 隐私权利相关状态
const privacySuccess = ref('');
const privacyError = ref('');
const exporting = ref(false);
const withdrawing = ref(false);
const deletingAccount = ref(false);

const form = reactive({
  name: '',
  gender: '',
  birthDate: '',
  phone: '',
  email: '',
  currentCity: '',
  nativePlace: '',
});

/** 加载个人信息 */
async function loadProfile() {
  loading.value = true;
  try {
    const res = await api.get('/profile');
    if (res.data) {
      form.name = res.data.name || '';
      form.gender = res.data.gender || '';
      form.birthDate = res.data.birthDate || '';
      form.phone = res.data.phone || '';
      form.email = res.data.email || '';
      form.currentCity = res.data.currentCity || '';
      form.nativePlace = res.data.nativePlace || '';
      if (res.data.avatarUrl) {
        avatarPreview.value = res.data.avatarUrl;
      }
    }
  } catch (e) {
    error.value = e.response?.data?.msg || '加载个人信息失败';
  } finally {
    loading.value = false;
  }
}

/** 触发头像选择 */
function triggerAvatar() {
  avatarInput.value?.click();
}

/** 头像更改 */
function handleAvatarChange(e) {
  const file = e.target.files?.[0];
  if (!file) return;

  // 大小校验
  if (file.size > 2 * 1024 * 1024) {
    error.value = '头像文件大小不能超过 2MB';
    return;
  }

  avatarFile.value = file;
  const reader = new FileReader();
  reader.onload = (ev) => {
    avatarPreview.value = ev.target.result;
  };
  reader.readAsDataURL(file);

  // 清空 input 以便重复选择同一文件
  e.target.value = '';
}

/** 保存个人信息 */
async function handleSave() {
  error.value = '';
  success.value = '';
  saving.value = true;

  try {
    // 如有头像先上传
    let uploadedAvatarUrl = null;
    if (avatarFile.value) {
      const fd = new FormData();
      fd.append('file', avatarFile.value);
      const uploadRes = await api.post('/profile/avatar', fd);
      if (uploadRes.data?.url) {
        uploadedAvatarUrl = uploadRes.data.url;
        avatarPreview.value = uploadedAvatarUrl;
      }
    }

    // 构建请求体（仅在有真实头像值时才包含 avatarUrl）
    const body = {
      name: form.name,
      gender: form.gender,
      birthDate: form.birthDate,
      email: form.email,
      currentCity: form.currentCity,
      nativePlace: form.nativePlace,
    };
    const finalAvatarUrl = uploadedAvatarUrl || avatarPreview.value;
    if (finalAvatarUrl) {
      body.avatarUrl = finalAvatarUrl;
    }

    await api.put('/profile', body);

    success.value = '个人信息已保存';
    setTimeout(() => (success.value = ''), 3000);
  } catch (e) {
    error.value = e.response?.data?.msg || '保存失败，请稍后重试';
  } finally {
    saving.value = false;
  }
}

/** 导出个人数据 */
async function handleExportData() {
  privacyError.value = '';
  privacySuccess.value = '';

  if (!confirm('确认导出您的个人数据？系统将生成一份包含您所有个人信息的副本供下载。')) return;

  exporting.value = true;
  try {
    const res = await api.get('/profile/export', { responseType: 'blob' });
    // 如果后端支持直接返回 blob
    const blob = res instanceof Blob ? res : new Blob([JSON.stringify(res, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `个人数据导出_${new Date().toISOString().slice(0, 10)}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    privacySuccess.value = '数据导出成功';
    setTimeout(() => (privacySuccess.value = ''), 3000);
  } catch (e) {
    privacyError.value = e.response?.data?.msg || '导出失败，请稍后重试';
  } finally {
    exporting.value = false;
  }
}

/** 撤回同意 */
async function handleWithdrawConsent() {
  privacyError.value = '';
  privacySuccess.value = '';

  if (!confirm(
    '确认撤回对个人信息收集和使用的同意？\n\n' +
    '撤回同意后，我们将停止处理您的个人信息，' +
    '但这可能影响您使用投递岗位等核心功能。' +
    '您仍可以浏览岗位信息。'
  )) return;

  withdrawing.value = true;
  try {
    await api.post('/profile/withdraw-consent');
    privacySuccess.value = '同意已撤回。如需继续使用核心功能，可重新授权。';
  } catch (e) {
    privacyError.value = e.response?.data?.msg || '操作失败，请稍后重试';
  } finally {
    withdrawing.value = false;
  }
}

/** 注销账号 */
async function handleDeleteAccount() {
  privacyError.value = '';
  privacySuccess.value = '';

  // 二次确认
  if (!confirm(
    '⚠️ 确认注销账号？\n\n' +
    '此操作将永久删除您的账号、个人资料、简历附件和所有投递记录。' +
    '此操作不可撤销！\n\n' +
    '确认要继续吗？'
  )) return;

  // 最终确认（输入确认文字）
  const finalInput = prompt('请输入"确认注销"以继续：');
  if (finalInput !== '确认注销') {
    privacyError.value = '注销已取消（未输入正确的确认文字）';
    return;
  }

  deletingAccount.value = true;
  try {
    await api.delete('/profile/account');
    // 清除本地登录态
    localStorage.clear();
    privacySuccess.value = '账号已注销，即将跳转...';
    setTimeout(() => {
      router.push('/login');
    }, 1500);
  } catch (e) {
    privacyError.value = e.response?.data?.msg || '注销失败，请稍后重试';
  } finally {
    deletingAccount.value = false;
  }
}

onMounted(() => {
  loadProfile();
  nextTick(updateIndicator);
});
</script>

<style scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 24px;
}

.page-title {
  font-size: 28px;
  margin-bottom: 24px;
  color: var(--color-text);
}

/* ===== Tab 滚动容器（移动端适配） ===== */
.tabs-scroll {
  position: relative;
  margin-bottom: 24px;
  /* 移动端横向滚动 */
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;          /* Firefox */
  -ms-overflow-style: none;       /* IE/Edge */
  /* 顶部 sticky 时需考虑刘海屏 */
  padding-top: env(safe-area-inset-top, 0px);
}
.tabs-scroll::-webkit-scrollbar {
  display: none;                  /* Chrome/Safari */
}

/* ===== Tab 列表（flex 容器） ===== */
.tabs {
  display: flex;
  position: relative;
  border-bottom: 1px solid var(--color-border);
  /* no gap — 指示器宽度基于 offsetLeft+offsetWidth，gap 会导致计算偏差 */
}

/* ===== 单个 Tab ===== */
.tab {
  position: relative;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;               /* iOS HIG 最小触摸目标 */
  padding: 12px 18px;
  font-size: 14px;
  font-weight: 400;
  color: var(--color-text-secondary);
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
  user-select: none;
  -webkit-user-select: none;
  -webkit-tap-highlight-color: transparent;
  transition: color 0.2s ease;
}
.tab:hover {
  color: var(--color-text);
}
.tab:active {
  /* 触摸反馈 */
  background: rgba(95, 184, 214, 0.08);
  transition: background 0.05s;
}
.tab--active {
  color: var(--color-primary);
  font-weight: 600;
}

/* ===== Active 指示器（独立元素，CSS transition 动画） ===== */
.tab-indicator {
  position: absolute;
  bottom: -1px;                   /* 覆盖 .tabs 的 border-bottom */
  left: 0;
  height: 3px;
  background: var(--color-primary);
  border-radius: 2px 2px 0 0;
  pointer-events: none;           /* 不阻挡 Tab 点击 */
  transition: left 0.3s cubic-bezier(0.4, 0, 0.2, 1),
              width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  will-change: left, width;       /* GPU 加速 */
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

/* 卡片 */
.profile-card {
  background: var(--bg-glass);
  backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--color-border);
  border-radius: var(--radius);
  padding: 32px;
}

/* 头像 */
.avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-border);
}
.avatar-preview {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid var(--color-border);
  transition: border-color 0.2s;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}
.avatar-preview:hover {
  border-color: var(--color-primary);
}
.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-placeholder {
  font-size: 28px;
  color: var(--color-primary);
  font-weight: 600;
}
.avatar-info {
  flex: 1;
}
.avatar-title {
  font-size: 14px;
  color: var(--color-text);
  margin-bottom: 4px;
}
.avatar-hint {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.avatar-file-input {
  display: none;
}

/* 表单行 */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
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
.form-group input[type="email"],
.form-group input[type="tel"],
.form-group input[type="date"] {
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
.input-readonly {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 性别单选 */
.radio-group {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-top: 8px;
}
/* 用更高特异性覆盖 .form-group label { display:block }，确保 flex + gap 生效 */
.form-group .radio-label {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;                    /* radio 与文本间距，稍宽松避免拥挤 */
  cursor: pointer;
  font-size: 14px;
  color: var(--color-text);
  line-height: 1.2;
  white-space: nowrap;          /* 防止"男/女"文本被压缩换行 */
  margin-bottom: 0;
}
.radio-label input[type="radio"] {
  flex-shrink: 0;               /* 圆形控件不被压缩 */
  width: 16px;
  height: 16px;
  margin: 0;
  vertical-align: middle;
  accent-color: var(--color-primary);
}
.radio-label span {
  flex-shrink: 0;
}

/* 按钮 */
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

/* 隐私设置 */
.section-title {
  font-size: 18px;
  margin-bottom: 6px;
  color: var(--color-text);
}
.section-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}
.privacy-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.privacy-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
}
.privacy-item:last-child {
  border-bottom: none;
}
.privacy-item-text {
  flex: 1;
}
.privacy-item-title {
  font-size: 14px;
  color: var(--color-text);
  margin-bottom: 4px;
}
.privacy-item-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
}

/* 开关 */
.toggle-switch {
  position: relative;
  width: 44px;
  height: 24px;
  flex-shrink: 0;
  margin-left: 16px;
}
.toggle-switch input {
  display: none;
}
.toggle-slider {
  display: block;
  width: 100%;
  height: 100%;
  background: var(--color-border);
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}
.toggle-slider::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 20px;
  height: 20px;
  background: #fff;
  border-radius: 50%;
  transition: transform 0.2s;
}
.toggle-switch input:checked + .toggle-slider {
  background: var(--color-primary);
}
.toggle-switch input:checked + .toggle-slider::after {
  transform: translateX(20px);
}
.toggle-switch input:disabled + .toggle-slider {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
  .profile-card {
    padding: 20px;
  }
  .rights-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .btn-rights {
    width: 100%;
  }
}

/* ====== 个人信息权利 ====== */
.rights-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.rights-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
  gap: 16px;
}
.rights-item:last-child {
  border-bottom: none;
}
.rights-item__text {
  flex: 1;
}
.rights-item__title {
  font-size: 14px;
  color: var(--color-text);
  margin-bottom: 4px;
}
.rights-item__desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

/* 权利操作按钮 */
.btn-rights {
  flex-shrink: 0;
  padding: 10px 24px;
  border: 1px solid var(--color-primary);
  border-radius: var(--radius);
  background: transparent;
  color: var(--color-primary);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: inherit;
}
.btn-rights:hover:not(:disabled) {
  background: rgba(95, 184, 214, 0.1);
  transform: translateY(-1px);
}
.btn-rights:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-rights--warning {
  border-color: #E8A33D;
  color: #E8A33D;
}
.btn-rights--warning:hover:not(:disabled) {
  background: rgba(232, 163, 61, 0.1);
}
.btn-rights--danger {
  border-color: var(--color-danger);
  color: var(--color-danger);
}
.btn-rights--danger:hover:not(:disabled) {
  background: rgba(224, 82, 82, 0.1);
}

/* ===== 移动端触摸优化 ===== */
@media (max-width: 767px) {
  .profile-page {
    padding: 24px var(--container-px) 60px;
  }
  .page-title {
    font-size: 22px;
  }

  /* 表单输入防iOS缩放 */
  .form-group input[type="text"],
  .form-group input[type="email"],
  .form-group input[type="tel"],
  .form-group input[type="date"] {
    min-height: var(--input-min-h);
    font-size: 16px;
  }

  /* 提交按钮全宽 */
  .btn-submit {
    width: 100%;
    min-height: var(--touch-min);
  }

  /* 双列表单 → 单列 */
  .form-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }


  /* 性别 Radio 组间距加大 */
  .radio-group {
    gap: 32px;
  }
  .form-group .radio-label {
    min-height: var(--touch-min);
    font-size: 16px;
    display: inline-flex;
    align-items: center;
    line-height: 1.2;
  }

  /* 头像区域移动端适配 */
  .avatar-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  /* 个人信息权利项 */
  .rights-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .btn-rights {
    width: 100%;
    text-align: center;
    justify-content: center;
    min-height: var(--touch-min);
  }

  /* 隐私设置项 */
  .privacy-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding: 14px 0;
  }
  .toggle-switch {
    align-self: flex-start;
  }
}
</style>
