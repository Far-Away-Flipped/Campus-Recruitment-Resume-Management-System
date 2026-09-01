import axios from 'axios';
import { toastError } from './toast';

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
  response => {
    const { code, msg } = response.data;
    if (code === 200) return response.data;
    // 业务错误（后端恒为 HTTP 200，通过 body.code 区分）——统一弹提示并 reject
    const err = new Error(msg || '请求失败');
    // 保留原始业务数据，供视图 catch 按 code 分支做差异化处理
    err.response = { data: response.data, status: response.status, config: response.config };
    // 20001 走下方 error 拦截器的 refresh 流程，不在此处弹"登录过期"
    if (code !== 20001) toastError(msg || '请求失败');
    return Promise.reject(err);
  },
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
    } else if (!error.response) {
      // 网络层错误（超时/断网等），无后端业务码——统一提示
      toastError('网络错误，请稍后重试');
    }
    return Promise.reject(error);
  }
);

export default api;
