/**
 * Vue Router 配置文件。
 * 负责定义应用的路由规则、导航守卫等。
 * createWebHistory 用于创建 HTML5 history 模式的路由。
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 初始路由定义
const routes: Array<RouteRecordRaw> = [
  {
    path: '/login', // 登录页面的路径
    name: 'Login', // 路由名称，方便编程式导航
    // 页面组件将通过动态导入实现懒加载
    // 我们将在 views 目录下创建 LoginPage.vue
    component: () => import('../views/LoginPage.vue') 
  },
  {
    path: '/register', // 注册页面的路径
    name: 'Register', // 路由名称
    // 页面组件将通过动态导入实现懒加载
    // 我们将在 views 目录下创建 RegisterPage.vue
    component: () => import('../views/RegisterPage.vue')
  },
  {
    // 仪表盘或主功能页的路径，可以先设置为根路径 '/'
    // 或者如果希望登录后才访问，可以设置为如 '/dashboard'
    // 这里我们先设置为 /dashboard，并假设它是受保护的 (后续会添加导航守卫)
    path: '/dashboard', 
    name: 'Dashboard', // 路由名称
    // 页面组件将通过动态导入实现懒加载
    // 我们将在 views 目录下创建 DashboardPage.vue
    component: () => import('../views/DashboardPage.vue'),
    // meta 字段可以用来存储路由元信息，例如该路由是否需要认证
    meta: { requiresAuth: true } // 标记此路由需要用户认证
  },
  // 添加一个重定向，使得访问根路径 '/' 时自动跳转到仪表盘
  // 这通常在用户已登录的情况下比较友好
  // 如果用户未登录，后续的导航守卫会将其重定向到登录页
  {
    path: '/',
    redirect: '/dashboard' // 当访问根路径时，重定向到 /dashboard
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL), // 使用 Vite 的 BASE_URL
  routes
})

export default router 