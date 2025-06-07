/**
 * Vue 应用的主入口文件。
 * 负责初始化Vue实例、Pinia状态管理、Vue Router路由、
 * Naive UI组件库以及全局样式。
 */
import { createApp } from 'vue' // 导入 Vue 的 createApp 方法
import { createPinia } from 'pinia' // 导入 Pinia 的 createPinia 方法
import naive from 'naive-ui'// 导入 Naive UI
import './style.css'// 导入全局样式
import router from './router'// 导入路由配置
import App from './App.vue'// 导入根组件

const app = createApp(App) // 创建 Vue 应用实例

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue全局错误处理:', err, info);
  // 如果是parentNode相关错误，不让它中断应用
  if (err instanceof Error && err.message.includes('parentNode')) {
    console.warn('检测到parentNode错误，已被全局错误处理器捕获');
    return;
  }
};

// 捕获未处理的Promise拒绝
window.addEventListener('unhandledrejection', (event) => {
  console.error('未处理的Promise拒绝:', event.reason);
  // 如果是轮询中断错误，不显示给用户
  if (event.reason instanceof Error && event.reason.message === '轮询已被中断') {
    event.preventDefault();
    console.log('轮询中断错误已被处理');
    return;
  }
});

app.use(createPinia()) // 安装 Pinia 状态管理
app.use(router) // 安装路由
app.use(naive) // 安装 Naive UI

app.mount('#app') // 挂载到 #app 元素
