<template>
  <!-- Naive UI 的布局组件，提供基本的页面结构 -->
  <n-layout style="height: 100vh">
    <!-- 页眉区域 -->
    <n-layout-header style="height: 64px; padding: 0 24px; display: flex; align-items: center; border-bottom: 1px solid #eee;">
      <!-- 放置 Logo 或应用标题 -->
      <div style="font-size: 1.4em; font-weight: bold;">AI 业务支持平台</div>
      <!-- 导航链接等可以后续添加到这里 -->
    </n-layout-header>

    <!-- 主要内容区域 -->
    <n-layout-content style="padding: 24px;">
      <!-- 
        Vue Router 的核心组件，用于显示当前路由匹配到的组件。
        我们应用中的各个页面 (如登录页、仪表盘等) 将会在这里被渲染。
      -->
      <router-view />
    </n-layout-content>

    <!-- 页脚区域 (可选) -->
    <n-layout-footer style="height: 64px; padding: 24px; text-align: center; border-top: 1px solid #eee;">
      © {{ new Date().getFullYear() }} AI 业务支持平台. All Rights Reserved.
    </n-layout-footer>
  </n-layout>
</template>

<script setup lang="ts">
// 使用 <script setup> 语法，这是 Vue 3 推荐的组合式 API 写法，代码更简洁。
// 目前这个布局组件是纯静态的，不需要特定的逻辑，所以 script 部分为空。
// 后续如果需要在布局层面处理一些逻辑 (如获取用户信息、全局事件监听等)，会在这里添加。

// 导入 Naive UI 的布局组件，确保在模板中使用时 TypeScript 能正确识别类型。
// 虽然我们在 main.ts 中全局注册了 Naive UI，但在 <script setup> 中显式导入有时有助于类型推断和编辑器支持。
// 不过，对于全局注册的组件，通常在模板中直接使用即可，无需在此处单独导入。
// import { NLayout, NLayoutHeader, NLayoutContent, NLayoutFooter } from 'naive-ui';
</script>

<style scoped>
/* scoped 样式只会应用到当前组件的元素，避免全局样式污染。 */

/* 你可以在这里为布局添加特定的样式 */
.n-layout-header {
  /* background-color: #f0f2f5; */ /* 示例：页眉背景色 */
}

.n-layout-footer {
  color: #888;
}
</style> 