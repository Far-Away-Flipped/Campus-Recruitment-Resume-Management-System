<template>
  <div class="not-found-page">
    <div class="not-found-card">
      <div class="not-found-code">404</div>
      <h1 class="not-found-title">页面未找到</h1>
      <p class="not-found-desc">您访问的页面不存在或已被移除。请检查地址是否正确，或返回首页浏览其他内容。</p>
      <div class="not-found-actions">
        <router-link to="/" class="btn btn--primary">返回首页</router-link>
        <button class="btn btn--ghost" @click="$router.go(-1)">返回上页</button>
      </div>
    </div>
  </div>
</template>

<script setup>
// 无需额外逻辑
</script>

<style scoped>
.not-found-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
  padding: 40px 24px;
}

.not-found-card {
  text-align: center;
  max-width: 480px;
}

.not-found-code {
  font-size: 120px;
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--color-primary);
  opacity: 0.15;
  line-height: 1;
  margin-bottom: 8px;
  user-select: none;
}

.not-found-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 12px;
}

.not-found-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  line-height: 1.7;
  margin-bottom: 32px;
}

.not-found-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  flex-wrap: wrap;
}

.btn {
  display: inline-block;
  padding: 10px 28px;
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.3s;
  border: none;
  font-family: inherit;
}
.btn--primary {
  background: linear-gradient(135deg, #5FB8D6, #6BB3FF);
  color: var(--color-bg);
}
.btn--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(95, 184, 214, 0.3);
}
.btn--ghost {
  background: transparent;
  border: 1px solid var(--color-border);
  color: var(--color-text-secondary);
}
.btn--ghost:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

@media (max-width: 480px) {
  .not-found-code {
    font-size: 80px;
  }
  .not-found-actions {
    flex-direction: column;
  }
  .btn {
    width: 100%;
    text-align: center;
  }
}
</style>
