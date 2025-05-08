// 从 Pinia 导入 defineStore 函数，用于定义一个新的 store
import { defineStore } from 'pinia';

// --- 类型定义 ---

/**
 * 用户信息对象的接口。
 * 后续可以根据后端返回的用户信息结构进行扩展。
 */
interface UserInfo {
  id: number | string; // 用户ID
  username: string;    // 用户名
  email?: string;       // 邮箱 (可选)
  // ... 其他用户信息字段
}

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
    user: null as UserInfo | null, // 使用类型断言指定 user 可以是 UserInfo 类型或 null
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
     * 清除 Token 和用户信息。
     */
    logout() {
      this.setToken(null); // 调用 setToken 并传入 null 来清除 Token
      this.setUser(null);  // 清除用户信息
      // 可以在这里添加额外的逻辑，例如重定向到登录页 (通常在调用此 action 的组件中处理)
      // router.push({ name: 'Login' }); // 示例：如果 router 实例在这里可用
    },

    /**
     * 初始化认证状态 (通常在应用启动时调用)。
     * 这个 action 目前只是一个占位符，因为 token 的加载已在 state 中通过 localStorage 完成。
     * 后续可以扩展此方法，例如：
     * - 验证 localStorage 中 Token 的有效性 (例如，通过向后端发送一个请求)。
     * - 如果 Token 有效，则获取最新的用户信息并更新 store。
     */
    initializeAuth() {
      if (this.token) {
        // console.log('Auth initialized with token from localStorage:', this.token);
        // TODO: 后续可以添加逻辑，例如根据 Token 获取用户信息
        // try {
        //   const userData = await fetchUserProfile(); // 假设有一个获取用户信息的 API 调用
        //   this.setUser(userData);
        // } catch (error) {
        //   console.error('Failed to fetch user profile on init:', error);
        //   this.logout(); // 如果获取失败，可能意味着 Token 无效，则登出
        // }
      } else {
        // console.log('No token found in localStorage during auth initialization.');
      }
    },

    // login(credentials: LoginCredentials): Promise<void> { ... }
    // register(registrationData: RegistrationData): Promise<void> { ... }
    // 这些 action 将在后续步骤中实现，用于处理实际的登录和注册 API 调用
  },
}); 