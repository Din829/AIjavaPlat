<template>
  <!-- Naive UI 的布局组件，提供基本的页面结构 -->
  <n-layout style="height: 100vh">
    <!-- 页眉区域 -->
    <n-layout-header class="app-header" style="height: 64px; padding: 0 24px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eee;">
      <!-- 放置 Logo 或应用标题 -->
      <div style="font-size: 1.4em; font-weight: bold; cursor: pointer;" @click="goToDashboard">
        AI 业务支持平台
      </div>

      <!-- 主导航菜单 - 仅在用户已登录时显示 -->
      <n-menu v-if="authStore.isAuthenticated" mode="horizontal" :options="menuOptions" />

      <n-space align="center">
        <!-- 如果用户已认证 -->
        <template v-if="authStore.isAuthenticated && authStore.currentUser">
          <n-dropdown trigger="hover" :options="userDropdownOptions" @select="handleUserDropdownSelect">
            <n-button text>
              <n-avatar round size="small" style="margin-right: 8px; background-color: #18a058;">
                {{ authStore.currentUser.username?.charAt(0).toUpperCase() }}
              </n-avatar>
              你好, {{ authStore.currentUser.username }}
            </n-button>
          </n-dropdown>
        </template>
        <!-- 如果用户未认证 -->
        <template v-else>
          <router-link to="/login" custom v-slot="{ navigate }">
            <n-button @click="navigate" quaternary>登录</n-button>
          </router-link>
          <router-link to="/register" custom v-slot="{ navigate }">
            <n-button @click="navigate" quaternary>注册</n-button>
          </router-link>
        </template>
      </n-space>
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
import { h } from 'vue';
import { useRouter, RouterLink } from 'vue-router';
import { useAuthStore } from '../stores/authStore';
import { useMessage } from 'naive-ui';
import type { DropdownOption, MenuOption } from 'naive-ui'; // 单独导入类型
import { ref } from 'vue';
import { onMounted } from 'vue';
import { NLayout, NLayoutHeader, NLayoutSider, NLayoutContent, NLayoutFooter, NMenu, NButton, NSpace, NIcon } from 'naive-ui';
import { PersonCircleOutline as UserIcon, LogOutOutline as LogoutIcon, HomeOutline as DashboardIcon, KeyOutline as TokenIcon } from '@vicons/ionicons5'; // 引入图标

const router = useRouter();
const authStore = useAuthStore();
const message = useMessage();

const goToDashboard = () => {
  router.push({ name: 'Dashboard' });
};

// 导航菜单选项
const menuOptions: MenuOption[] = [
  {
    label: () => h(
      RouterLink,
      { to: { name: 'Dashboard' } },
      { default: () => '仪表盘' }
    ),
    key: 'dashboard'
  },
  {
    label: () => h(
      RouterLink,
      { to: { name: 'Tokens' } },
      { default: () => 'API Token管理' }
    ),
    key: 'tokens'
  },
  // --- 修改 Prompt 管理导航项 ---
  {
    label: () => h(
      RouterLink,
      { to: { name: 'Prompts' } }, // 链接到 Prompts 路由
      { default: () => 'Prompt管理' }
    ),
    key: 'prompts',
    // disabled: true // 移除禁用状态
  },
  {
    label: () => h(
      'span',
      { style: 'color: #999;' }, // 未实现的功能显示为灰色
      '网页摘要'
    ),
    key: 'summarize',
    disabled: true // 暂时禁用
  }
];

// 用户下拉菜单选项
const userDropdownOptions: DropdownOption[] = [
  {
    label: '登出',
    key: 'logout',
    // 可选：添加图标
    // icon: () => h('i', { class: 'i-logout-icon' }) // 假设你有一个登出图标的 CSS 类
  }
];

const handleUserDropdownSelect = (key: string | number) => {
  if (key === 'logout') {
    handleLogout();
  }
};

const handleLogout = () => {
  authStore.logout();
  message.success('您已成功登出！');
  router.push({ name: 'Login' });
};

// --- 响应式状态 ---
const collapsed = ref(false); // 侧边栏是否折叠

// --- 生命周期钩子 ---
onMounted(async () => {
  // 检查是否存在 token
  if (authStore.token) {
    console.log('[AppLayout] Token exists on mount. Attempting to fetch user info...');
    try {
      // 如果存在 token，则尝试获取用户信息
      await authStore.fetchAndSetUser();
      console.log('[AppLayout] User info fetch attempt completed.');
    } catch (error) {
      // fetchAndSetUser 内部会处理错误并登出
      console.error('[AppLayout] Error during initial user fetch:', error);
    }
  } else {
    console.log('[AppLayout] No token found on mount.');
  }
});
</script>

<style scoped>
/* scoped 样式只会应用到当前组件的元素，避免全局样式污染。 */

/* 页眉样式 */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

/* 导航菜单样式 */
:deep(.n-menu) {
  flex: 1;
  margin: 0 20px;
}

:deep(.n-menu .n-menu-item-content) {
  padding: 0 16px;
}

/* 移除了图标相关样式 */

/* 页脚样式 */
.n-layout-footer {
  color: #888;
  text-align: center;
}

/* 可以为 router-link 按钮调整样式，使其更像普通链接（如果需要）*/
.n-button[quaternary] {
  font-weight: normal;
}
</style>