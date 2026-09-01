import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import './assets/styles/global.css';

const app = createApp(App);

// 注册 motion-fade 自定义指令（IntersectionObserver 驱动滑入）
app.directive('motion-fade', {
  mounted(el, binding) {
    const { y = 24, delay = 0 } = binding.value || {};
    el.style.opacity = '0';
    el.style.transform = `translateY(${y}px)`;
    el.style.transition = 'opacity 0.8s ease, transform 0.8s ease';
    el.style.transitionDelay = `${delay}s`;
    const io = new IntersectionObserver((entries, obs) => {
      if (entries[0].isIntersecting) {
        el.style.opacity = '1';
        el.style.transform = 'translateY(0)';
        obs.disconnect();
      }
    }, { threshold: 0.1 });
    io.observe(el);
    el._motionIo = io;
  },
  unmounted(el) {
    if (el._motionIo) el._motionIo.disconnect();
  }
});

app.use(createPinia());
app.use(router);
app.mount('#app');
