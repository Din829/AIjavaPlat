// 导入我们将在阶段4创建的全局 Axios 实例
// import apiClient from './apiClient'; // apiClient 将封装基础 URL、拦截器等

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
  username: string;  // 用户名
  email: string;     // 邮箱
  password: string;  // 密码
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

// --- 服务方法 ---

/**
 * 用户登录服务方法。
 * @param {LoginCredentials} credentials - 用户的登录凭据 (用户名/邮箱和密码)。
 * @returns {Promise<AuthResponse>} 一个 Promise，解析为认证响应数据 (包含 accessToken)。
 * @throws {Error} 如果登录失败或发生网络错误。
 */
export const loginUser = async (credentials: LoginCredentials): Promise<AuthResponse> => {
  console.log('[authService] Attempting to log in with:', credentials);
  // TODO: 后续替换为实际的 API 调用
  // try {
  //   // 使用 apiClient 发送 POST 请求到后端的登录API
  //   // 路径通常是 /api/auth/login (基础URL /api 已在 apiClient 中配置)
  //   const response = await apiClient.post<AuthResponse>('/auth/login', credentials);
  //   return response.data; // 返回响应体中的数据 (AuthResponse)
  // } catch (error) {
  //   // apiClient 中的响应拦截器应该已经处理了通用的网络错误和HTTP状态码错误
  //   // 但这里可以针对登录特定的错误做进一步处理或直接抛出，由调用方 (authStore) 捕获
  //   console.error('[authService] Login failed:', error);
  //   // 抛出一个更具体的错误信息，或者让 apiClient 的错误处理机制来处理
  //   throw new Error('登录失败，请检查您的凭据或网络连接。'); 
  // }

  // --- 占位逻辑，用于当前阶段测试 --- 
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (credentials.usernameOrEmail === 'testuser' && credentials.password === 'password') {
        console.log('[authService] Mock login successful.');
        resolve({ accessToken: 'mock-jwt-token-for-' + credentials.usernameOrEmail });
      } else {
        console.error('[authService] Mock login failed: Invalid credentials.');
        reject(new Error('模拟登录失败：无效的凭据。'));
      }
    }, 1000); // 模拟网络延迟
  });
  // --- 占位逻辑结束 ---
};

/**
 * 用户注册服务方法。
 * @param {RegistrationData} userData - 用户的注册信息。
 * @returns {Promise<any>} 一个 Promise，解析为注册成功后的响应 (具体结构待后端定义，可能为空或包含用户信息)。
 * @throws {Error} 如果注册失败或发生网络错误。
 */
export const registerUser = async (userData: RegistrationData): Promise<any> => {
  console.log('[authService] Attempting to register user:', userData);
  // TODO: 后续替换为实际的 API 调用
  // try {
  //   // 使用 apiClient 发送 POST 请求到后端的注册API
  //   // 路径通常是 /api/auth/register
  //   const response = await apiClient.post<any>('/auth/register', userData);
  //   return response.data; // 返回响应体
  // } catch (error) {
  //   console.error('[authService] Registration failed:', error);
  //   throw new Error('注册失败，请稍后重试。');
  // }

  // --- 占位逻辑，用于当前阶段测试 --- 
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (userData.username && userData.email && userData.password) {
        // 简单模拟，不检查用户名或邮箱是否已存在
        console.log('[authService] Mock registration successful for:', userData.username);
        resolve({ message: '模拟注册成功！' }); // 模拟成功响应
      } else {
        console.error('[authService] Mock registration failed: Missing data.');
        reject(new Error('模拟注册失败：缺少必要信息。'));
      }
    }, 1000); // 模拟网络延迟
  });
  // --- 占位逻辑结束 ---
};

// 未来可能添加的其他认证相关服务方法：
// - refreshToken(): 用于刷新 JWT Token
// - forgotPassword(email: string): 忘记密码
// - resetPassword(token: string, newPassword: string): 重置密码
// - fetchUserProfile(): 获取当前登录用户的详细信息 (如果登录时不直接返回) 