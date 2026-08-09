<template>
  <header class="portal-header">
    <div class="header-inner">
      <!-- 汉堡按钮（移动端） -->
      <button
        class="hamburger"
        :class="{ 'hamburger--active': drawerOpen }"
        @click="toggleDrawer"
        aria-label="打开导航菜单"
      >
        <span></span>
        <span></span>
        <span></span>
      </button>

      <router-link to="/" class="logo">
        <span class="logo-text">ATMOTO</span>
        <span class="logo-sub">遨天科技 · 校园招聘</span>
      </router-link>

      <!-- 桌面端导航 -->
      <nav class="nav-links">
        <router-link to="/jobs">岗位浏览</router-link>
        <router-link to="/my-applications" v-if="auth.isLoggedIn">我的投递</router-link>
        <router-link to="/messages" v-if="auth.isLoggedIn">消息</router-link>
        <router-link to="/privacy">隐私政策</router-link>
      </nav>

      <!-- 桌面端操作按钮 -->
      <div class="header-actions">
        <template v-if="auth.isLoggedIn">
          <router-link to="/profile" class="btn-ghost">{{ auth.user?.name || '个人中心' }}</router-link>
          <button class="btn-ghost" @click="auth.logout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login" class="btn-ghost">登录</router-link>
          <router-link to="/register" class="btn-primary">注册</router-link>
        </template>
      </div>
    </div>

    <!-- 移动端抽屉导航 -->
    <Teleport to="body">
      <Transition name="drawer-slide">
        <aside v-if="drawerOpen" class="drawer" @click.stop>
          <div class="drawer__header">
            <router-link to="/" class="drawer__logo" @click="closeDrawer">
              <span class="drawer__logo-text">ATMOTO</span>
              <span class="drawer__logo-sub">遨天科技 · 校园招聘</span>
            </router-link>
            <button class="drawer__close" @click="closeDrawer" aria-label="关闭导航菜单">
              <span></span>
              <span></span>
            </button>
          </div>

          <nav class="drawer__nav">
            <router-link to="/jobs" @click="closeDrawer">岗位浏览</router-link>
            <router-link to="/my-applications" v-if="auth.isLoggedIn" @click="closeDrawer">我的投递</router-link>
            <router-link to="/messages" v-if="auth.isLoggedIn" @click="closeDrawer">消息</router-link>
            <router-link to="/privacy" @click="closeDrawer">隐私政策</router-link>
          </nav>

          <div class="drawer__actions">
            <template v-if="auth.isLoggedIn">
              <router-link to="/profile" class="drawer__btn drawer__btn--ghost" @click="closeDrawer">
                {{ auth.user?.name || '个人中心' }}
              </router-link>
              <button class="drawer__btn drawer__btn--ghost" @click="auth.logout(); closeDrawer()">退出</button>
            </template>
            <template v-else>
              <router-link to="/login" class="drawer__btn drawer__btn--ghost" @click="closeDrawer">登录</router-link>
              <router-link to="/register" class="drawer__btn drawer__btn--primary" @click="closeDrawer">注册</router-link>
            </template>
          </div>

          <!-- 安全区域占位 -->
          <div class="drawer__safe-area"></div>
        </aside>
      </Transition>
      <Transition name="drawer-fade">
        <div v-if="drawerOpen" class="drawer-overlay" @click="closeDrawer"></div>
      </Transition>
    </Teleport>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '../stores/auth.js';
const auth = useAuthStore();

const isMobile = ref(false);
const drawerOpen = ref(false);

function checkMobile() {
  isMobile.value = window.matchMedia('(max-width: 767px)').matches;
  if (!isMobile.value && drawerOpen.value) {
    closeDrawer();
  }
}

function toggleDrawer() {
  drawerOpen.value = !drawerOpen.value;
  if (drawerOpen.value) {
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
}

function closeDrawer() {
  drawerOpen.value = false;
  document.body.style.overflow = '';
}

function onKeydown(e) {
  if (e.key === 'Escape' && drawerOpen.value) {
    closeDrawer();
  }
}

// 初始化
checkMobile();

const mediaQuery = window.matchMedia('(max-width: 767px)');
mediaQuery.addEventListener('change', checkMobile);

onMounted(() => {
  document.addEventListener('keydown', onKeydown);
});

onUnmounted(() => {
  mediaQuery.removeEventListener('change', checkMobile);
  document.removeEventListener('keydown', onKeydown);
  document.body.style.overflow = '';
});
</script>

<style scoped>
/* ===== 桌面端 Header 布局（默认样式） ===== */
.portal-header {
  background: rgba(10, 14, 23, 0.95);
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: var(--container-max);
  margin: 0 auto;
  padding: 0 var(--container-px);
  height: 64px;
  display: flex;
  align-items: center;
  gap: 32px;
}
.logo {
  display: flex;
  align-items: baseline;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}
.logo-text {
  font-family: var(--font-mono);
  font-size: 22px;
  color: var(--color-primary);
  letter-spacing: 2px;
}
.logo-sub {
  font-size: 13px;
  color: var(--color-text-secondary);
}
.nav-links {
  display: flex;
  gap: 24px;
  flex: 1;
}
.nav-links a {
  color: var(--color-text-secondary);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}
.nav-links a:hover,
.nav-links a.router-link-exact-active {
  color: var(--color-primary);
}
.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
.btn-ghost {
  background: none;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
  padding: 6px 16px;
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 14px;
  text-decoration: none;
  transition: all 0.2s;
}
.btn-ghost:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.btn-primary {
  background: var(--color-primary);
  border: none;
  color: var(--color-bg);
  padding: 6px 20px;
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: opacity 0.2s;
}
.btn-primary:hover {
  opacity: 0.85;
}

/* ===== 汉堡按钮（移动端默认显示，桌面端隐藏） ===== */
.hamburger {
  display: none;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 5px;
  width: 48px;
  height: 48px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  flex-shrink: 0;
}
.hamburger span {
  display: block;
  width: 22px;
  height: 2px;
  background: var(--color-text);
  border-radius: 2px;
  transition: transform 0.3s ease, opacity 0.3s ease;
  transform-origin: center;
}
.hamburger--active span:nth-child(1) {
  transform: translateY(7px) rotate(45deg);
}
.hamburger--active span:nth-child(2) {
  opacity: 0;
}
.hamburger--active span:nth-child(3) {
  transform: translateY(-7px) rotate(-45deg);
}

/* ===== 抽屉导航（移动端） ===== */
.drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: min(300px, 85vw);
  background: var(--color-card);
  z-index: 201;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}
.drawer__logo {
  display: flex;
  flex-direction: column;
  text-decoration: none;
}
.drawer__logo-text {
  font-family: var(--font-mono);
  font-size: 20px;
  color: var(--color-primary);
  letter-spacing: 2px;
}
.drawer__logo-sub {
  font-size: 11px;
  color: var(--color-text-secondary);
}
.drawer__close {
  width: 48px;
  height: 48px;
  background: none;
  border: none;
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.drawer__close span {
  position: absolute;
  width: 20px;
  height: 2px;
  background: var(--color-text-secondary);
  border-radius: 2px;
}
.drawer__close span:first-child {
  transform: rotate(45deg);
}
.drawer__close span:last-child {
  transform: rotate(-45deg);
}
.drawer__nav {
  display: flex;
  flex-direction: column;
  padding: 12px 0;
}
.drawer__nav a {
  display: flex;
  align-items: center;
  min-height: 48px;
  padding: 14px 20px;
  font-size: 16px;
  color: var(--color-text-secondary);
  text-decoration: none;
  border-left: 3px solid transparent;
  transition: all 0.2s;
}
.drawer__nav a:hover,
.drawer__nav a.router-link-exact-active {
  color: var(--color-primary);
  background: rgba(95, 184, 214, 0.06);
  border-left-color: var(--color-primary);
}
.drawer__actions {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}
.drawer__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 16px;
  font-family: inherit;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.2s;
}
.drawer__btn--ghost {
  background: none;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
}
.drawer__btn--ghost:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}
.drawer__btn--primary {
  background: var(--color-primary);
  border: none;
  color: var(--color-bg);
  font-weight: 600;
}
.drawer__btn--primary:hover {
  opacity: 0.85;
}
.drawer__safe-area {
  height: var(--safe-bottom);
  flex-shrink: 0;
}

/* 遮罩 */
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
}

/* 抽屉过渡动画 */
.drawer-slide-enter-active,
.drawer-slide-leave-active {
  transition: transform 0.3s ease;
}
.drawer-slide-enter-from,
.drawer-slide-leave-to {
  transform: translateX(100%);
}
.drawer-fade-enter-active,
.drawer-fade-leave-active {
  transition: opacity 0.3s ease;
}
.drawer-fade-enter-from,
.drawer-fade-leave-to {
  opacity: 0;
}

/* ===== 移动端自适应 ===== */
@media (max-width: 767px) {
  .header-inner {
    height: var(--header-h, 56px);
    gap: 12px;
    padding: 0 16px;
  }
  .hamburger {
    display: flex;
  }
  .nav-links,
  .header-actions {
    display: none;
  }
  .logo {
    flex: 1;
    justify-content: center;
  }
  .logo-text {
    font-size: 18px;
  }
  .logo-sub {
    font-size: 11px;
  }
}

/* ===== 桌面端恢复（隐藏汉堡） ===== */
@media (min-width: 768px) {
  .hamburger {
    display: none;
  }
  .header-inner {
    height: 64px;
  }
}
</style>
