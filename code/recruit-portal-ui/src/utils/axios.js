import axios from 'axios';

const api = axios.create({
  baseURL: '/api/portal',
  timeout: 10000,
});

api.interceptors.request.use(config => {
  const token = localStorage.getItem('access_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  response => response.data,
  async error => {
    if (error.response?.data?.code === 20001) {
      // token过期，尝试refresh
      const refreshToken = localStorage.getItem('refresh_token');
      if (refreshToken) {
        try {
          const res = await axios.post('/api/portal/auth/refresh', { refreshToken });
          localStorage.setItem('access_token', res.data.data.accessToken);
          localStorage.setItem('refresh_token', res.data.data.refreshToken);
          error.config.headers.Authorization = `Bearer ${res.data.data.accessToken}`;
          return axios(error.config);
        } catch {
          localStorage.clear();
          window.location.href = '/login';
        }
      } else {
        localStorage.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default api;
