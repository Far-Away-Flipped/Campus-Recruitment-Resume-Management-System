<template>
  <footer class="portal-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <p class="footer-slogan">赋能人类太空活动</p>
        <p class="footer-copyright">&copy; 2026 遨天科技（北京）有限公司 版权所有</p>
        <a class="footer-icp" href="https://beian.miit.gov.cn/" target="_blank">京ICP备20000185号-1</a>
      </div>
      <div class="footer-links">
        <h4>快速链接</h4>
        <router-link to="/">招聘岗位</router-link>
        <a href="http://www.at-moto.com/home" target="_blank">公司官网</a>
        <router-link to="/privacy">隐私政策</router-link>
      </div>
      <div class="footer-contact">
        <h4>联系方式</h4>
        <p>邮箱：<a href="mailto:AT-HR@atmoto.cn">AT-HR@atmoto.cn</a></p>
        <p>地址：北京市海淀区上地信息路12号 中关村发展大厦二层E座</p>
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
  padding: 40px 0 24px;
  margin-top: auto;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 40px;
}

/* 品牌区 */
.footer-brand {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.footer-slogan {
  font-size: 14px;
  color: var(--color-primary);
  font-weight: 600;
  letter-spacing: 2px;
  margin-bottom: 8px;
}
.footer-copyright {
  font-size: 12px;
  color: var(--color-text-secondary);
}
.footer-icp {
  font-size: 12px;
  color: var(--color-text-secondary);
  text-decoration: none;
}
.footer-icp:hover {
  color: var(--color-primary);
}

/* 快速链接 */
.footer-links h4,
.footer-contact h4 {
  font-size: 13px;
  color: var(--color-text);
  margin-bottom: 12px;
  letter-spacing: 1px;
  font-weight: 600;
}
.footer-links a {
  display: block;
  font-size: 13px;
  color: var(--color-text-secondary);
  text-decoration: none;
  margin-bottom: 8px;
  transition: color 0.2s;
}
.footer-links a:hover {
  color: var(--color-primary);
}

/* 联系方式 */
.footer-contact p {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
  line-height: 1.6;
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
    grid-template-columns: 1fr;
    gap: 20px;
    text-align: center;
    padding: 0 16px;
  }
  .portal-footer {
    padding: 28px 0 20px;
  }
  .footer-brand {
    align-items: center;
  }
}
</style>
