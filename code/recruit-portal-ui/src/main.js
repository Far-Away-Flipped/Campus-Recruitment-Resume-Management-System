import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import './assets/styles/global.css';

const app = createApp(App);

// 注册 motion-fade 自定义指令（CSS 动画由 IntersectionObserver 驱动）
app.directive('motion-fade', {
  mounted(el) {
    el.style.opacity = '0';
    el.style.transform = 'translateY(24px)';
    el.style.transition = 'opacity 0.8s ease, transform 0.8s ease';
  }
});

app.use(createPinia());
app.use(router);
app.mount('#app');
