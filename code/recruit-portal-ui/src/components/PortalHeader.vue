<template>
  <header class="portal-header">
    <div class="header-inner">
      <router-link to="/" class="logo">
        <span class="logo-text">ATMOTO</span>
        <span class="logo-sub">遨天科技 · 校园招聘</span>
      </router-link>
      <nav class="nav-links">
        <router-link to="/jobs">岗位浏览</router-link>
        <router-link to="/my-applications" v-if="auth.isLoggedIn">我的投递</router-link>
        <router-link to="/messages" v-if="auth.isLoggedIn">消息</router-link>
        <router-link to="/privacy">隐私政策</router-link>
      </nav>
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
  </header>
</template>

<script setup>
import { useAuthStore } from '../stores/auth.js';
const auth = useAuthStore();
</script>

<style scoped>
.portal-header {
  background: rgba(10, 14, 23, 0.95);
  border-bottom: 1px solid var(--color-border);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
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
</style>
