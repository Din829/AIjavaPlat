// 从 Pinia 导入 defineStore 函数，用于定义一个新的 store
import { defineStore } from 'pinia';
// 从 authService 导入登录服务和相关类型
import { 
  loginUser, 
  registerUser, 
  fetchCurrentUser,
  type LoginCredentials, 
  type RegistrationData, 
  type AuthResponse, 
  type UserInfo
} from '../services/authService';
// 导入其他store，用于在登出时重置状态
import { useTokenStore } from './tokenStore';

// --- 类型定义 ---

/**
 * JWT Token 在 localStorage 中存储的键名。
 * 使用常量可以避免硬编码字符串，方便后续修改。
 */
const TOKEN_STORAGE_KEY = 'authToken';

// --- Store 定义 ---

/**
 * 定义名为 'auth' 的 Pinia store。
 * 第一个参数是 store 的唯一 ID，Pinia 用它来连接 store 到 devtools。
 * 第二个参数是一个包含 state, getters, 和 actions 的对象。
 */
export const useAuthStore = defineStore('auth', {
  /**
   * State: 定义 store 的响应式数据属性。
   * 这里我们用一个函数返回初始状态对象，这对于 SSR (服务端渲染) 是必要的，
   * 并且也是 Pinia 推荐的做法，以确保每个 store 实例都有独立的状态。
   */
  state: () => ({
    /**
     * JWT Token，用于用户认证。
     * 初始值尝试从 localStorage 获取，如果不存在则为 null。
     * 这样即使用户刷新页面，只要 Token 未过期，登录状态就可以保持。
     */
    token: localStorage.getItem(TOKEN_STORAGE_KEY) || null,
    /**
     * 当前登录用户的信息。
     * 初始为 null，在用户成功登录并获取到用户信息后会被设置。
     */
    user: null as UserInfo | null,
    // 添加一个状态来跟踪用户信息是否正在加载
    isUserLoading: false,
  }),

  /**
   * Getters: 类似于 Vue 组件的计算属性 (computed properties)。
   * 它们可以用来从 state 派生出一些值，例如基于 token 判断用户是否已认证。
   */
  getters: {
    /**
     * 计算用户是否已认证。
     * 如果 token 存在 (不为 null 或空字符串)，则认为用户已认证。
     * this 指向当前的 store 实例。
     */
    isAuthenticated: (state) => !!state.token, // 使用 !! 将 token 转换为布尔值

    /**
     * 获取当前用户的信息。
     */
    currentUser: (state) => state.user,
  },

  /**
   * Actions: 定义可以修改 state 的方法。
   * Actions 可以是同步的，也可以是异步的 (例如 API 调用)。
   */
  actions: {
    /**
     * 设置认证 Token。
     * @param {string | null} newToken - 新的 JWT Token，如果为 null 则表示清除 Token。
     */
    setToken(newToken: string | null) {
      this.token = newToken; // 更新 store 中的 token
      if (newToken) {
        // 如果有新的 Token，将其存储到 localStorage 中以实现持久化登录
        localStorage.setItem(TOKEN_STORAGE_KEY, newToken);
      } else {
        // 如果 Token 为 null (例如登出时)，则从 localStorage 中移除
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      }
    },

    /**
     * 设置当前登录用户的信息。
     * @param {UserInfo | null} newUserInfo - 新的用户信息对象，或 null。
     */
    setUser(newUserInfo: UserInfo | null) {
      this.user = newUserInfo;
    },

    /**
     * 用户登出操作。
     * 清除 Token 和用户信息，同时重置其他store的状态。
     */
    logout() {
      // 清除认证信息
      this.setToken(null); // 调用 setToken 并传入 null 来清除 Token
      this.setUser(null);  // 清除用户信息

      // 重置其他store的状态
      // 注意：这里不能直接在顶层导入tokenStore，因为会导致循环依赖
      // 而是在方法内部获取store实例
      const tokenStore = useTokenStore();
      tokenStore.resetState(); // 重置Token状态

      // 重置加载状态
      this.isUserLoading = false;

      // 可以在这里添加额外的逻辑，例如重定向到登录页 (通常在调用此 action 的组件中处理)
      // router.push({ name: 'Login' }); // 示例：如果 router 实例在这里可用
    },

    /**
     * 尝试从后端获取当前用户信息并更新 store。
     * 通常在应用加载时，如果存在 token，则调用此 action。
     * @returns {Promise<void>}
     */
    async fetchAndSetUser(): Promise<void> {
      // 如果没有 token，或者已经在加载用户，则不执行
      if (!this.token || this.isUserLoading) {
        return;
      }
      
      this.isUserLoading = true;
      try {
        // 调用服务获取用户信息
        const userInfo = await fetchCurrentUser();
        // 更新 store 中的用户信息
        this.setUser(userInfo);
        console.log('[authStore] User info successfully fetched and set.', this.user);
      } catch (error) {
        console.error('[authStore] Failed to fetch user info:', error);
        // 获取用户信息失败，很可能是 token 无效或过期
        // 在这种情况下，执行登出操作来清理状态
        this.logout();
      } finally {
        this.isUserLoading = false;
      }
    },

    /**
     * 用户登录 Action。
     * @param {LoginCredentials} credentials - 用户的登录凭据。
     * @returns {Promise<void>} 一个 Promise，在登录成功时 resolve，在失败时 reject (抛出错误)。
     */
    async login(credentials: LoginCredentials): Promise<void> {
      try {
        const authResponse: AuthResponse = await loginUser(credentials);
        this.setToken(authResponse.accessToken);
        
        // 登录成功后，立即尝试获取完整的用户信息
        // 注意：不依赖模拟设置用户，而是调用 fetchAndSetUser
        // this.setUser({ ... }); // 移除模拟设置
        await this.fetchAndSetUser(); // 获取并设置真实用户数据
        
        // 如果 fetchAndSetUser 失败（例如并发问题或后端错误），
        // login action 仍然会成功（因为 token 已设置），但 user 可能仍然是 null。
        // 这通常没问题，因为后续访问页面时守卫会检查。
        
      } catch (error) {
        console.error('[authStore] Login action failed:', error);
        throw error;
      }
    },

    /**
     * 用户注册 Action。
     * @param {RegistrationData} registrationData - 用户的注册信息。
     * @returns {Promise<void>} 一个 Promise，在注册成功时 resolve，在失败时 reject (抛出错误)。
     */
    async register(registrationData: RegistrationData): Promise<void> {
      try {
        const authResponse = await registerUser(registrationData);
        this.setToken(authResponse.accessToken);
        
        // 注册成功后，同样立即尝试获取完整的用户信息
        // this.setUser({ ... }); // 移除模拟设置
        await this.fetchAndSetUser(); // 获取并设置真实用户数据
        
      } catch (error) {
        console.error('[authStore] Register action failed:', error);
        throw error;
      }
    }

    // login(credentials: LoginCredentials): Promise<void> { ... }
    // register(registrationData: RegistrationData): Promise<void> { ... }
    // 这些 action 将在后续步骤中实现，用于处理实际的登录和注册 API 调用
  },
});