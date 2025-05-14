/**
 * Vue Router 配置文件。
 * 负责定义应用的路由规则、导航守卫等。
 * createWebHistory 用于创建 HTML5 history 模式的路由。
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/authStore'; // 导入 auth store

// 路由定义
const routes: Array<RouteRecordRaw> = [
  // 公共路由（不需要认证）
  {
    path: '/login', // 登录页面的路径
    name: 'Login', // 路由名称，方便编程式导航
    // 页面组件将通过动态导入实现懒加载
    component: () => import('../views/LoginPage.vue')
  },
  {
    path: '/register', // 注册页面的路径
    name: 'Register', // 路由名称
    component: () => import('../views/RegisterPage.vue')
  },

  // 需要认证的路由
  {
    path: '/dashboard',
    name: 'Dashboard', // 仪表盘页面
    component: () => import('../views/DashboardPage.vue'),
    meta: { requiresAuth: true } // 标记此路由需要用户认证
  },
  {
    path: '/tokens',
    name: 'Tokens', // API Token管理页面
    component: () => import('../views/TokensPage.vue'),
    meta: { requiresAuth: true } // 标记此路由需要用户认证
  },
  {
    path: '/prompts',
    name: 'Prompts',
    component: () => import('../views/PromptsPage.vue'),
    meta: { requiresAuth: true, title: 'Prompt 管理' }
  },
  {
    path: '/summarize',
    name: 'Summarize',
    component: () => import('../views/SummarizationPage.vue'),
    meta: { requiresAuth: true, title: '网页摘要' }
  },

  // 重定向
  {
    path: '/',
    redirect: '/dashboard' // 当访问根路径时，重定向到 /dashboard
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL), // 使用 Vite 的 BASE_URL
  routes
})

// 全局前置导航守卫
router.beforeEach(async (to, from, next) => {
  // 在导航守卫内部获取 authStore 实例
  // Pinia store 通常在 Vue 应用实例创建并使用 Pinia 插件后才可用。
  // 在 router.ts 中，尤其是在应用首次加载路由解析时，
  // 直接在模块顶层调用 useAuthStore() 可能过早。
  // 将其放在 beforeEach 回调内部，可以确保在需要时才尝试获取。
  const authStore = useAuthStore();

  // --- 添加调试日志 ---
  console.log('%c[Router Guard]','color: blue; font-weight: bold;', {
    path: to.path,
    requiresAuth: to.meta.requiresAuth,
    token: authStore.token ? authStore.token.substring(0, 10) + '...' : null, // 只打印部分 token
    isAuthenticated: authStore.isAuthenticated,
    currentUser: authStore.currentUser
  });
  // --- 结束调试日志 ---

  const requiresAuth = to.meta.requiresAuth;
  const isAuthenticated = authStore.isAuthenticated;

  // 如果目标路由需要认证
  if (requiresAuth) {
    if (isAuthenticated) {
      // 用户已认证，允许访问
      next();
    } else {
      // 用户未认证，重定向到登录页
      // 可以传递一个 query 参数，以便登录后重定向回原始页面 (可选)
      // next({ name: 'Login', query: { redirect: to.fullPath } });
      next({ name: 'Login' });
    }
  } else {
    // 如果目标路由不需要认证 (例如登录页、注册页)
    // 可选：如果用户已认证，并且尝试访问登录或注册页，则重定向到仪表盘
    if ((to.name === 'Login' || to.name === 'Register') && isAuthenticated) {
      next({ name: 'Dashboard' });
    } else {
      // 其他情况（不需要认证的页面，或者已登录用户访问非登录/注册的公共页面），直接允许访问
      next();
    }
  }
});

export default router