import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import path from 'path';

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ],
  resolve: { alias: { '@': path.resolve(__dirname, 'src') } },
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            // 剥离浏览器发送的 Origin 头，避免后端的 CorsFilter 因白名单不匹配而拒绝请求。
            // Vite 代理是服务端到服务端的请求，不需要 CORS。
            proxyReq.removeHeader('Origin');
          });
        }
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `$brand-primary: #5FB8D6;`
      }
    }
  }
});
