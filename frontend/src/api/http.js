import axios from 'axios';
import { ElMessage } from 'element-plus';

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('clip_hub_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const message = error?.response?.data?.message || error?.message || '请求失败';
    ElMessage.error(message);
    return Promise.reject(error);
  }
);

export default http;
