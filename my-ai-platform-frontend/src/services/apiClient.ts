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
    console.error('API request error:', {
      message: error.message,
      url: error.config?.url,
      method: error.config?.method,
      status: error.response?.status,
      data: error.response?.data,
      requestData: error.config?.data,
      stack: error.stack // 对于非HTTP错误或请求设置阶段的错误，stack可能更有用
    });

    // 检查是否是 /summarize 路径的特定业务错误 (如400 Bad Request)
    // 对于这类错误，我们希望业务逻辑层 (store) 来处理用户提示，而不是apiClient弹出通用提示
    if (error.config?.url?.endsWith('/summarize') && error.response?.status === 400) {
      // 直接将后端返回的业务错误信息向上抛，让store处理
      const serviceErrorMessage = error.response.data?.message || error.message || '摘要服务发生未知错误。';
      return Promise.reject(new Error(serviceErrorMessage)); 
    } else {
      // 对于其他错误，继续现有的全局提示逻辑
      if (error.response) {
        const { status, data } = error.response;

        // 从 FRONTEND_PLAN.md 整理的错误处理逻辑
        switch (status) {
          case 400:
            // Bad Request / Validation Error
            if (data?.errors && Array.isArray(data.errors)) {
              message.error(data.errors[0]?.defaultMessage || '请求参数无效');
            } else {
              message.error(data?.message || '请求无效或参数错误');
            }
            console.warn('[API Client] Bad Request (400):', data);
            break;
          case 401:
            message.error(data?.message || '认证失败，请重新登录');
            if (error.config.url?.endsWith('/api/auth/login') === false) {
              useAuthStore().logout();
            } else {
              console.warn('[API Client] Login attempt failed (401).');
            }
            break;
          case 403:
            message.error(data?.message || '您没有权限执行此操作');
            console.warn('[API Client] Forbidden (403):', data);
            break;
          case 404:
            message.error(data?.message || '请求的资源未找到');
            console.warn('[API Client] Not Found (404):', data);
            break;
          case 422:
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
    }

    // 重要的是将错误再次抛出，这样具体的API调用点 (例如 service 文件或 store action) 才能捕获并处理它
    return Promise.reject(error);
  }
);

export default apiClient;