<!--
/**
 * Vue 应用的根组件 (Root Component)。
 * 这个组件现在的作用是加载并显示主要的应用程序布局 (AppLayout)。
 * AppLayout 组件内部会包含 <router-view> 来渲染具体的页面。
 */
-->
<template>
  <!-- Naive UI 的消息提示提供者，用于在应用中显示全局消息 (n-message) -->
  <!-- 它应该包裹在应用的根部，或者至少在需要使用 n-message 的组件的共同祖先处 -->
  <n-message-provider>
    <!-- Naive UI 的对话框提供者，用于在应用中使用全局对话框 (n-dialog) -->
    <n-dialog-provider>
      <!-- Naive UI 的通知提供者，用于在应用中使用全局通知 (n-notification) -->
      <n-notification-provider>
        <!-- Naive UI 的加载条提供者，用于在应用顶部显示全局加载进度条 (n-loading-bar) -->
        <n-loading-bar-provider>
          <!-- 我们的主应用布局 -->
          <AppLayout />
        </n-loading-bar-provider>
      </n-notification-provider>
    </n-dialog-provider>
  </n-message-provider>
</template>

<script setup lang="ts">
/**
 * 5. 导入组件和资源
 * - 导入 HelloWorld 组件
 * - 导入 Vue logo SVG 文件（通过 import 语句）
 * - 这种方式让 TypeScript 能够正确识别类型
 */
// import HelloWorld from './components/HelloWorld.vue'
// 导入 Vue logo SVG 文件
// import vueLogo from './assets/vue.svg'

// 导入主应用布局组件
import AppLayout from './layouts/AppLayout.vue';

// 导入 Naive UI 的全局内容 API 组件，这些组件需要在应用的根部进行包裹，以便在任何地方调用它们的 API
// 例如，通过 useMessage(), useDialog() 等 hooks。
import { 
  NMessageProvider, 
  NDialogProvider, 
  NNotificationProvider,
  NLoadingBarProvider
} from 'naive-ui';

// App.vue 通常不需要太多其他逻辑，它的主要职责是设置好顶层结构。
</script>

<style>
/* 
  这里的样式是全局样式，会影响整个应用。
  Vite 模板默认有一个 style.css，通常在这里引入或直接编写全局样式。
  我们已经在 main.ts 中导入了 './style.css'，所以这里可以保持简洁，
  或者只放置那些真正需要在 App.vue 这个顶层组件应用的特定全局覆盖。
  为了避免意外，通常更推荐将所有全局样式集中在 main.ts 导入的那个 CSS 文件中。
*/

/* 移除 Vite 模板默认的 logo 和 HelloWorld 相关样式，因为我们不再使用它们 */
/* 保留一些基本的全局样式设置可能是好的，比如 body 的 margin */
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}
</style>
