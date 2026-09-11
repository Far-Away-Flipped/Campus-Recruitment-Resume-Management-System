import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import './assets/styles/global.css';

const app = createApp(App);

// 注册 motion-fade 自定义指令（IntersectionObserver 驱动滑入）
//
// 两条硬约束（移动端踩过的坑，改动前务必读懂）：
// 1. 不能用 >0 的 threshold 判定"进入视口"。intersectionRatio 的上限是
//    视口高度 / 元素高度，超过约 10 倍视口的元素（隐私政策、长表单等）
//    永远达不到阈值 → opacity 卡在 0，整页空白。
// 2. 动画结束后必须清掉内联 transform。任何非 none 的 transform 都会让该元素
//    成为 position:fixed 后代的包含块，页内弹窗/底部抽屉/固定 CTA 会被定位到
//    "页面盒子"而非视口，在手机上直接跑到屏幕外。
app.directive('motion-fade', {
  mounted(el, binding) {
    const { y = 24, delay = 0 } = binding.value || {};

    // 系统开启"减少动态效果" → 直接显示，不做动画
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return;
    if (typeof IntersectionObserver === 'undefined') return;

    el.style.opacity = '0';
    el.style.transform = `translateY(${y}px)`;
    el.style.transition = 'opacity 0.8s ease, transform 0.8s ease';
    el.style.transitionDelay = `${delay}s`;

    let done = false;
    const reveal = () => {
      if (done) return;
      done = true;
      el.style.opacity = '1';
      el.style.transform = 'translateY(0)';

      // 动画结束后清空内联样式：解除 transform 对 position:fixed 的包含块效应
      const cleanup = () => {
        el.style.opacity = '';
        el.style.transform = '';
        el.style.transition = '';
        el.style.transitionDelay = '';
        el.removeEventListener('transitionend', cleanup);
      };
      el.addEventListener('transitionend', cleanup);
      // transitionend 在元素动画被打断/不可见时不保证触发，加定时兜底
      el._motionCleanupTimer = setTimeout(cleanup, delay * 1000 + 1000);
    };

    // 挂载时已在视口内（含"元素比视口高得多"的情况）→ 立即显示，不等 IO
    const rect = el.getBoundingClientRect();
    const vh = window.innerHeight || document.documentElement.clientHeight;
    if (rect.top < vh && rect.bottom > 0) {
      reveal();
      return;
    }

    // 尚未进入视口 → 等滚动进入（threshold 0：只要露头就显示）
    const io = new IntersectionObserver((entries, obs) => {
      if (entries.some((e) => e.isIntersecting)) {
        obs.disconnect();
        reveal();
      }
    }, { threshold: 0 });
    io.observe(el);
    el._motionIo = io;
    el._motionReveal = reveal;
  },
  unmounted(el) {
    if (el._motionIo) el._motionIo.disconnect();
    if (el._motionCleanupTimer) clearTimeout(el._motionCleanupTimer);
  }
});

app.use(createPinia());
app.use(router);
app.mount('#app');
