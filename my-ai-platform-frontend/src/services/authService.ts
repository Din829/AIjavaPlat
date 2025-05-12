// 导入全局 Axios 实例
import apiClient from './apiClient'; // 封装基础 URL、拦截器等

// --- 类型定义 (与后端API的DTO对应) ---

/**
 * 登录请求凭据的接口。
 * 与后端 AuthController 中的 LoginRequestDto 对应。
 */
export interface LoginCredentials {
  usernameOrEmail: string; // 用户名或邮箱
  password: string;        // 密码
}

/**
 * 注册请求数据的接口。
 * 与后端 AuthController 中的 RegisterRequestDto 对应。
 */
export interface RegistrationData {
  username: string;       // 用户名
  email: string;          // 邮箱
  password: string;       // 密码
  confirmPassword: string; // 确认密码
}

/**
 * 认证响应数据的接口 (例如登录成功后返回的数据)。
 * 与后端 AuthController 中的 AuthResponseDto 对应。
 */
export interface AuthResponse {
  accessToken: string; // JWT Token
  // tokenType?: string; // 通常是 'Bearer'，可能包含在响应中
  // expiresIn?: number; // Token 过期时间 (秒)
  // user?: UserInfo; // 可能直接在登录响应中返回用户信息
}

/**
 * 用户信息 DTO 接口。
 * 与后端的 UserDto 对应。
 */
export interface UserInfo {
  id: number | string; // 注意：这里保持与之前authStore一致，可以是 string 或 number
  username: string;
  email?: string;
  createdAt?: string; // 可选，根据后端返回添加
  updatedAt?: string; // 可选，根据后端返回添加
}

// --- 服务方法 ---

/**
 * 用户登录服务方法。
 * @param {LoginCredentials} credentials - 用户的登录凭据 (用户名/邮箱和密码)。
 * @returns {Promise<AuthResponse>} 一个 Promise，解析为认证响应数据 (包含 accessToken)。
 * @throws {Error} 如果登录失败或发生网络错误。
 */
export const loginUser = async (credentials: LoginCredentials): Promise<AuthResponse> => {
  console.log('[authService] Attempting to log in with:', credentials.usernameOrEmail);
  try {
    // 使用 apiClient 发送 POST 请求到后端的登录API
    // 路径是 /api/auth/login (基础URL已在 apiClient 中配置)
    const response = await apiClient.post<AuthResponse>('/api/auth/login', credentials);
    return response.data; // 返回响应体中的数据 (AuthResponse)
  } catch (error) {
    // apiClient 中的响应拦截器应该已经处理了通用的网络错误和HTTP状态码错误
    // 但这里可以针对登录特定的错误做进一步处理或直接抛出，由调用方 (authStore) 捕获
    console.error('[authService] Login failed:', error);
    // 抛出一个更具体的错误信息，或者让 apiClient 的错误处理机制来处理
    throw new Error('登录失败，请检查您的凭据或网络连接。');
  }

  // --- 以下是之前的模拟实现，现在已替换为真实API调用 ---
  // return new Promise((resolve, reject) => {
  //   setTimeout(() => {
  //     if (credentials.usernameOrEmail === 'testuser' && credentials.password === 'password') {
  //       console.log('[authService] Mock login successful.');
  //       resolve({ accessToken: 'mock-jwt-token-for-' + credentials.usernameOrEmail });
  //     } else {
  //       console.error('[authService] Mock login failed: Invalid credentials.');
  //       reject(new Error('模拟登录失败：无效的凭据。'));
  //     }
  //   }, 1000); // 模拟网络延迟
  // });
};

/**
 * 用户注册服务方法。
 * @param {RegistrationData} userData - 用户的注册信息。
 * @returns {Promise<AuthResponse>} 一个 Promise，解析为注册成功后的响应 (包含accessToken)。
 * @throws {Error} 如果注册失败或发生网络错误。
 */
export const registerUser = async (userData: RegistrationData): Promise<AuthResponse> => {
  console.log('[authService] Attempting to register user:', userData.username);
  try {
    // 使用 apiClient 发送 POST 请求到后端的注册API
    // 路径是 /api/auth/register (基础URL已在 apiClient 中配置)
    const response = await apiClient.post<AuthResponse>('/api/auth/register', userData);
    return response.data; // 返回响应体
  } catch (error: any) {
    console.error('[authService] Registration failed:', error);

    // 尝试提取更详细的错误信息
    let errorMessage = '注册失败，请稍后重试。';

    if (error.response) {
      // 服务器返回了错误响应
      const { status, data } = error.response;

      if (typeof data === 'string') {
        // 如果后端直接返回字符串错误信息
        errorMessage = data;
      } else if (data && data.message) {
        // 如果后端返回了包含message字段的JSON
        errorMessage = data.message;
      } else if (status === 400) {
        errorMessage = '注册信息无效，请检查您的输入。';
      } else if (status === 409) {
        errorMessage = '用户名或邮箱已被使用。';
      }
    } else if (error.request) {
      // 请求已发出但没有收到响应
      errorMessage = '无法连接到服务器，请检查您的网络连接。';
    }

    throw new Error(errorMessage);
  }

  // --- 以下是之前的模拟实现，现在已替换为真实API调用 ---
  // return new Promise((resolve, reject) => {
  //   setTimeout(() => {
  //     if (userData.username && userData.email && userData.password) {
  //       // 简单模拟，不检查用户名或邮箱是否已存在
  //       console.log('[authService] Mock registration successful for:', userData.username);
  //       resolve({ message: '模拟注册成功！' }); // 模拟成功响应
  //     } else {
  //       console.error('[authService] Mock registration failed: Missing data.');
  //       reject(new Error('模拟注册失败：缺少必要信息。'));
  //     }
  //   }, 1000); // 模拟网络延迟
  // });
};

/**
 * 获取当前登录用户的详细信息。
 * @returns {Promise<UserInfo>} 一个 Promise，解析为用户信息。
 * @throws {Error} 如果获取失败或用户未认证。
 */
export const fetchCurrentUser = async (): Promise<UserInfo> => {
  console.log('[authService] Attempting to fetch current user info...');
  try {
    // 调用后端的 /api/users/me 接口
    const response = await apiClient.get<UserInfo>('/api/users/me');
    console.log('[authService] Fetched user info:', response.data);
    return response.data;
  } catch (error) {
    // apiClient 中的响应拦截器应该会处理 401 等错误
    // 但这里仍然可以记录错误并抛出，以便调用方知道获取失败
    console.error('[authService] Failed to fetch current user:', error);
    throw new Error('获取用户信息失败');
  }
};

// 未来可能添加的其他认证相关服务方法：
// - refreshToken(): 用于刷新 JWT Token
// - forgotPassword(email: string): 忘记密码
// - resetPassword(token: string, newPassword: string): 重置密码