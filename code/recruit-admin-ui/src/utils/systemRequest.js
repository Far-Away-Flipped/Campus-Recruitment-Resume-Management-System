import axios from 'axios';
import { ElMessage } from 'element-plus';

const service = axios.create({
  baseURL: '/api/system',
  timeout: 15000,
});

service.interceptors.request.use(config => {
  const token = localStorage.getItem('admin_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

service.interceptors.response.use(
  response => {
    const { code, msg } = response.data;
    if (code === 200) return response.data;
    ElMessage.error(msg || '请求失败');
    return Promise.reject(new Error(msg));
  },
  error => {
    ElMessage.error('网络错误');
    return Promise.reject(error);
  }
);

export default service;
