<!-- @ts-nocheck -->
<template>
  <!-- 页面主容器，使用 Flexbox 实现内容垂直和水平居中 -->
  <div class="login-page-container">
    <!-- Naive UI 卡片组件，用于包裹登录表单，提供视觉上的分组和美化 -->
    <n-card title="欢迎登录 AI 业务支持平台" class="login-card">
      <!-- Naive UI 表单组件 -->
      <!-- ref="formRef" 用于获取表单实例，以便调用其方法 (如 validate) -->
      <!-- :model="loginForm" 将表单数据对象与表单关联 -->
      <!-- :rules="rules" 定义表单校验规则 -->
      <!-- @submit.prevent="handleLogin" 阻止表单默认提交行为，并调用 handleLogin 方法 -->
      <n-form ref="formRef" :model="loginForm" :rules="rules" @submit.prevent="handleLogin">
        <!-- 用户名或邮箱输入行 -->
        <!-- path="usernameOrEmail" 对应 rules 中的字段名，用于校验 -->
        <n-form-item-row label="用户名或邮箱" path="usernameOrEmail">
          <n-input
            v-model:value="loginForm.usernameOrEmail"
            placeholder="请输入您的用户名或邮箱"
            clearable
            @keydown.enter="handleLogin" /> <!-- 按回车键也触发表单提交 -->
        </n-form-item-row>

        <!-- 密码输入行 -->
        <n-form-item-row label="密码" path="password">
          <n-input
            type="password"
            v-model:value="loginForm.password"
            placeholder="请输入您的密码"
            show-password-on="mousedown"
            clearable
            @keydown.enter="handleLogin" />
        </n-form-item-row>

        <!-- 登录按钮 -->
        <!-- block 属性使按钮占据整行宽度 -->
        <!-- attr-type="submit" 将按钮类型设置为提交按钮，配合 form 的 @submit.prevent -->
        <!-- :loading="isLoading" 根据 isLoading 状态显示加载中效果 -->
        <!-- :disabled="isLoading" 在加载中时禁用按钮 -->
        <n-button
          type="primary"
          block
          attr-type="submit"
          :loading="isLoading"
          :disabled="isLoading">
          登录
        </n-button>
      </n-form>

      <!-- 分隔线 -->
      <n-divider />

      <!-- 注册链接区域 -->
      <div style="text-align: center;">
        还没有账户？
        <!-- router-link 用于客户端导航，to="/register" 指向注册页面的路由 -->
        <router-link to="/register">
          <n-button text type="primary">立即注册</n-button>
        </router-link>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck
// --- 依赖导入 ---

// 从 Vue 导入 ref 用于创建响应式数据引用，reactive 用于创建响应式对象
import { ref, reactive } from 'vue';
// 从 Vue Router 导入 useRouter 用于编程式导航 (例如，登录成功后跳转页面)
import { useRouter } from 'vue-router';
// 从 Naive UI 导入表单相关的类型 (FormInst, FormRules) 和 useMessage hook (用于显示全局提示)
// 以及模板中用到的 Naive UI 组件
import type { FormInst, FormRules } from 'naive-ui';
import { useMessage, NCard, NForm, NFormItemRow, NInput, NButton, NDivider } from 'naive-ui';

// 从我们的 Pinia store 中导入 useAuthStore，用于访问和操作认证状态
import { useAuthStore } from '../stores/authStore';
// 导入在 authService.ts 中定义的登录凭据接口类型，用于类型提示
// @ts-ignore - 这个类型在 loginForm 的类型注解中使用，但 TypeScript 可能会报告它未使用
import type { LoginCredentials } from '../services/authService';

// --- 组件状态定义 ---

// 获取 Vue Router 的实例，用于后续的页面跳转
const router = useRouter();
// 获取 Naive UI 的 message API 实例，用于显示如成功、错误等提示信息
const message = useMessage();
// 获取认证 store 的实例，以便访问其 state、getters 和 actions
const authStore = useAuthStore();

// 创建一个 ref 来引用模板中的 n-form 组件实例 (formRef)
// FormInst 是 Naive UI 表单实例的类型，初始值为 null
const formRef = ref<FormInst | null>(null);

// 使用 reactive 创建一个响应式对象来存储登录表单的数据
// 表单字段与模板中的 v-model:value 双向绑定
const loginForm = reactive<LoginCredentials>({ //明确指定类型为 LoginCredentials
  usernameOrEmail: '', // 用户名或邮箱，初始为空字符串
  password: '',       // 密码，初始为空字符串
});

// 创建一个 ref 来追踪当前的加载状态 (例如，当正在向服务器发送登录请求时)
// 初始为 false，表示未在加载
const isLoading = ref(false);

// --- 表单校验规则 ---

// 定义 Naive UI 表单的校验规则对象
// 键名 (如 'usernameOrEmail') 对应 loginForm 中的属性名和 n-form-item-row 的 path 属性
const rules: FormRules = {
  usernameOrEmail: [
    {
      required: true, // 表示此字段是必填的
      message: '请输入用户名或邮箱', // 当校验失败时显示的错误消息
      trigger: ['input', 'blur'], // 触发校验的事件：输入时 (input) 和失去焦点时 (blur)
    },
  ],
  password: [
    {
      required: true, // 密码字段也是必填的
      message: '请输入密码',
      trigger: ['input', 'blur'],
    },
  ],
};

// --- 事件处理方法 ---

/**
 * 处理登录表单提交的异步函数。
 * 当用户点击登录按钮或在输入框中按回车时触发。
 */
const handleLogin = async () => {
  // 如果当前正在加载 (例如，上一次登录请求还未完成)，则直接返回，防止重复提交
  if (isLoading.value) return;

  try {
    // 首先，调用表单实例的 validate 方法进行校验
    // formRef.value 可能为 null (理论上在组件挂载后不会，但类型上是可选的)，使用可选链 ?.
    // await 会等待校验完成，如果校验失败，validate 方法会抛出错误
    await formRef.value?.validate();

    // 如果校验通过，设置 isLoading 为 true，表示开始处理登录逻辑
    isLoading.value = true;
    // 打印表单数据到控制台，方便调试
    console.log('登录表单数据:', loginForm.usernameOrEmail, loginForm.password);

    // 【核心登录逻辑 - TODO: 下一步将这部分逻辑移入 authStore 的 action中】
    // 为了快速看到效果，这里暂时直接调用 authService 中的模拟登录方法
    // 最终目标：await authStore.login(loginForm);

    // 动态导入 authService (仅为临时演示，不推荐在生产代码中这样使用)
    // const { loginUser } = await import('../services/authService'); // 不再需要组件内直接导入
    try {
      // 调用 authStore 中的 login action
      await authStore.login({
        usernameOrEmail: loginForm.usernameOrEmail,
        password: loginForm.password,
      });

      // 登录成功后的操作由 authStore 的 login action 内部处理 token 和 user 设置
      // 组件层面只需要处理 UI 反馈 (message) 和路由跳转

      // 使用 Naive UI 的 message API 显示成功提示
      message.success('登录成功！正在跳转到仪表盘...');
      // 使用 Vue Router 跳转到仪表盘页面 (路由名称为 'Dashboard')
      router.push({ name: 'Dashboard' });

    } catch (error: any) {
      // 如果 authService.loginUser 抛出错误 (例如，模拟的凭据错误)
      console.error('登录服务调用失败:', error);
      // 显示错误消息
      message.error(error.message || '登录失败，请检查您的凭据或网络。');
    }
    // --- 模拟登录逻辑结束 ---

  } catch (validationErrors) {
    // 如果 formRef.value.validate() 校验失败，会捕获到错误
    // Naive UI 表单会自动在对应的表单项下显示错误信息，所以这里通常不需要额外做什么
    console.log('表单校验失败:', validationErrors);
    // message.warning('请检查表单输入项是否正确。'); // 可选：给出一个统一的校验失败提示
  } finally {
    // 无论登录成功、失败还是校验失败，最后都将 isLoading状态设置回 false
    isLoading.value = false;
  }
};
</script>

<style scoped>
/* scoped 样式只会应用到当前组件的元素，确保样式不会泄露到其他组件 */

/* 页面主容器样式 */
.login-page-container {
  display: flex; /* 使用 Flexbox 布局 */
  justify-content: center; /* 水平居中对齐子元素 (即登录卡片) */
  align-items: center;    /* 垂直居中对齐子元素 */
  /* 计算最小高度，使其至少撑满视口减去页眉和页脚的高度以及一些可能的padding，确保内容区域不会太小 */
  /* 这个计算需要根据 AppLayout.vue 中的实际高度调整 */
  min-height: calc(100vh - 64px - 64px - 48px); /* 示例：100vh - header(64) - footer(64) - 垂直padding(24*2) */
  /* background-color: #f7f8fa; */ /* 可选：为页面设置一个浅灰色背景 */
  padding: 20px; /* 给容器一些内边距，避免卡片紧贴边缘 */
}

/* 登录卡片样式 */
.login-card {
  width: 100%; /* 卡片宽度占满其父容器 (在小屏幕上) */
  max-width: 400px; /* 限制卡片的最大宽度，在大屏幕上不会过宽 */
  /* box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); */ /* 可选：为卡片添加一点阴影效果，增加立体感 */
}

/* 表单项之间的间距调整 */
/* Naive UI 的 n-form-item-row 默认可能没有底部外边距，或者间距不符合预期，可以手动调整 */
.login-card .n-form-item-row { /* 增加选择器权重，确保能覆盖 Naive UI 默认样式 */
  margin-bottom: 24px; /* 设置每个表单项下方的外边距 */
}

/* 最后一个表单项的底部外边距可以小一些，或者移除，因为它后面紧跟着按钮 */
.login-card .n-form-item-row:last-of-type { /* 仅针对表单中最后一个 n-form-item-row */
  margin-bottom: 28px; /* 或者根据视觉效果调整 */
}

/* 登录按钮的上边距，使其与最后一个表单项有一定间距 */
/* 通常 n-button 本身会有一些默认的边距，或者可以通过调整最后一个表单项的 margin-bottom 来控制 */
/* 如果直接控制按钮，可以这样： */
/*
.login-card .n-button[type="primary"] {
  margin-top: 10px;
}
*/
</style>