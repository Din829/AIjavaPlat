/**
 * Vue 应用的主入口文件。
 * 负责初始化Vue实例、Pinia状态管理、Vue Router路由、
 * Naive UI组件库以及全局样式。
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import naive from 'naive-ui'
import './style.css'
import router from './router'
import App from './App.vue'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(naive)

app.mount('#app')
