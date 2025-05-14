import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import { useAuthStore } from '../stores/authStore'; // 用于获取token和执行登出
import { message } from './messageService'; // 导入消息服务
// import router from '../router'; // 避免直接导入router以防循环依赖，登出后的跳转由authStore.logout的调用方处理

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 90000, // 设置全局超时为 90000 毫秒 (90 秒)
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const authStore = useAuthStore();
    const token = authStore.token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    // 对请求错误做些什么
    console.error('[API Client] Request error:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    // 对响应数据做点什么 (例如，如果所有成功的响应都包装在 data 属性中，可以直接返回 response.data)
    // 为了通用性，这里先返回整个 response，调用方可以自行处理 response.data
    return response;
  },
  (error) => {
    // 对响应错误做点什么
    console.error('[API Client] Response error:', error);
    const authStore = useAuthStore();

    if (error.response) {
      const { status, data } = error.response;

      // 从 FRONTEND_PLAN.md 整理的错误处理逻辑
      switch (status) {
        case 400:
          // Bad Request / Validation Error
          // 后端可能返回 { success: false, message: "..." } 或校验错误详情
          if (data?.errors && Array.isArray(data.errors)) {
            // 对于校验错误，通常由调用方在表单旁显示，这里只做通用提示
            message.error(data.errors[0]?.defaultMessage || '请求参数无效');
          } else {
            message.error(data?.message || '请求无效或参数错误');
          }
          console.warn('[API Client] Bad Request (400):', data);
          break;
        case 401:
          // Unauthorized
          message.error(data?.message || '认证失败，请重新登录');
          // 只有在不是登录请求本身失败时才执行登出和重定向，避免登录失败时无限循环
          if (error.config.url?.endsWith('/api/auth/login') === false) {
            authStore.logout(); // logout action 应该处理状态清除，调用方处理跳转
            // window.location.href = '/login'; // 或者直接强制跳转
          } else {
            console.warn('[API Client] Login attempt failed (401).');
          }
          break;
        case 403:
          // Forbidden
          message.error(data?.message || '您没有权限执行此操作');
          console.warn('[API Client] Forbidden (403):', data);
          break;
        case 404:
          // Not Found
          message.error(data?.message || '请求的资源未找到');
          console.warn('[API Client] Not Found (404):', data);
          break;
        case 422:
          // Unprocessable Entity (通常也是校验错误)
          message.error(data?.message || '请求参数验证失败');
          console.warn('[API Client] Unprocessable Entity (422):', data);
          break;
        case 500:
        case 501:
        case 502:
        case 503:
        case 504:
          // Server errors
          message.error(data?.message || '服务器发生错误，请稍后重试');
          console.error('[API Client] Server Error:', status, data);
          break;
        default:
          // 其他未特定处理的错误
          message.error(data?.message || '发生未知错误');
          console.error('[API Client] Unhandled HTTP Error:', status, data);
      }
    } else if (error.request) {
      // 请求已发出，但没有收到响应 (例如网络问题)
      message.error('网络错误，请检查您的连接');
      console.error('[API Client] Network error or no response:', error.request);
    } else {
      // 发送请求时出了点问题 (例如，配置错误)
      message.error('请求发送失败');
      console.error('[API Client] Request setup error:', error.message);
    }

    // 重要的是将错误再次抛出，这样具体的API调用点 (例如 service 文件或 store action) 才能捕获并处理它
    return Promise.reject(error);
  }
);

export default apiClient;