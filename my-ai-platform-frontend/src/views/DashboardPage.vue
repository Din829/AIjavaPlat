<template>
  <div>
    <h1>仪表盘 (Dashboard Page)</h1>
    <p>用户登录后看到的主页面，内容待开发...</p>
    <!-- 摘要功能、Token管理入口、Prompt管理入口等将在这里 -->
  </div>
</template>

<script setup lang="ts">
// 仪表盘页面的逻辑将在这里实现
// 例如，检查用户认证状态，获取需要展示的数据等
import { useAuthStore } from '../stores/authStore';

const authStore = useAuthStore();

// 可以在这里添加获取用户信息的逻辑等
console.log('Dashboard loaded. Current user:', authStore.currentUser);
</script>

<style scoped>
/* 仪表盘页面的局部样式 */
</style> 