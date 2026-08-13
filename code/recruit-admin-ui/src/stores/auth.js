import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import request from '@/utils/request';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('admin_token') || '');
  const userInfo = ref(null);
  const isSuperAdmin = computed(() => !!userInfo.value?.isSuperAdmin);

  async function login(username, password, captchaCode, captchaKey) {
    const res = await request.post('/auth/login', { username, password, captchaCode, captchaKey });
    const data = res.data;
    token.value = data.token;
    localStorage.setItem('admin_token', data.token);
    await getUserInfo();
    return data;
  }

  async function getUserInfo() {
    const res = await request.get('/auth/info');
    userInfo.value = res.data;
    return res.data;
  }

  function logout() {
    token.value = '';
    userInfo.value = null;
    localStorage.removeItem('admin_token');
  }

  return { token, userInfo, isSuperAdmin, login, getUserInfo, logout };
});
