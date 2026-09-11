import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      // 目标是 .env 里 PORTAL_PORT 对应的学生门户（nginx 托管静态资源 + 反代 /api/portal）。
      // 注意：必须指向门户而不是后端容器——后端不对外暴露端口，且 nginx 一层才有
      // /api/common、/api/admin 等路径的准入规则，绕过去就测不出真实行为。
      '/api': { target: 'http://127.0.0.1:8081', changeOrigin: true }
    }
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: { vendor: ['vue', 'vue-router', 'pinia'] }
      }
    }
  }
});
