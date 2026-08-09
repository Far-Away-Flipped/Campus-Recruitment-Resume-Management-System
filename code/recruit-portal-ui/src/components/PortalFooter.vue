<template>
  <footer class="portal-footer">
    <div class="footer-inner">
      <div class="footer-info">
        <p>&copy; 2026 遨天科技 版权所有</p>
        <p>京ICP备XXXXXXXX号</p>
      </div>
      <div class="footer-contact">
        <p>联系邮箱：<a href="mailto:AT-HR@atmoto.cn">AT-HR@atmoto.cn</a></p>
      </div>
    </div>
  </footer>
</template>

<script setup>
</script>

<style scoped>
.portal-footer {
  background: rgba(10, 14, 23, 0.9);
  border-top: 1px solid var(--color-border);
  padding: 24px 0;
  margin-top: auto;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.footer-info p, .footer-contact p {
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.8;
}
.footer-contact a {
  color: var(--color-primary);
  text-decoration: none;
}
.footer-contact a:hover {
  text-decoration: underline;
}

/* 移动端垂直堆叠 */
@media (max-width: 767px) {
  .footer-inner {
    flex-direction: column;
    gap: 12px;
    text-align: center;
    padding: 0 16px;
  }
  .portal-footer {
    padding: 20px 0;
  }
}
</style>
