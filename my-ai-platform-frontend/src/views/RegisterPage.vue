<template>
  <div class="register-page-container">
    <n-card title="创建您的账户" class="register-card">
      <n-form ref="formRef" :model="registerForm" :rules="rules" @submit.prevent="handleRegister">
        <n-form-item-row label="用户名" path="username">
          <n-input
            v-model:value="registerForm.username"
            placeholder="请输入用户名"
            clearable
            @keydown.enter="handleRegister" />
        </n-form-item-row>

        <n-form-item-row label="邮箱地址" path="email">
          <n-input
            v-model:value="registerForm.email"
            placeholder="请输入您的邮箱地址"
            clearable
            @keydown.enter="handleRegister" />
        </n-form-item-row>

        <n-form-item-row label="密码" path="password">
          <n-input
            type="password"
            v-model:value="registerForm.password"
            placeholder="请输入密码 (至少6位)"
            show-password-on="mousedown"
            clearable
            @keydown.enter="handleRegister" />
        </n-form-item-row>

        <n-form-item-row label="确认密码" path="confirmPassword">
          <n-input
            type="password"
            v-model:value="registerForm.confirmPassword"
            placeholder="请再次输入密码"
            show-password-on="mousedown"
            clearable
            @keydown.enter="handleRegister" />
        </n-form-item-row>

        <n-button
          type="primary"
          block
          attr-type="submit"
          :loading="isLoading"
          :disabled="isLoading">
          注册
        </n-button>
      </n-form>

      <n-divider />

      <div style="text-align: center;">
        已经有账户了？
        <router-link to="/login">
          <n-button text type="primary">立即登录</n-button>
        </router-link>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck // 暂时添加，如果遇到VLS类型问题，后续尝试解决
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInst, FormRules, FormItemRule } from 'naive-ui';
import { useMessage, NCard, NForm, NFormItemRow, NInput, NButton, NDivider } from 'naive-ui';
import { useAuthStore } from '../stores/authStore';
import type { RegistrationData } from '../services/authService';

const router = useRouter();
const message = useMessage();
const authStore = useAuthStore();

const formRef = ref<FormInst | null>(null);
const isLoading = ref(false);

// 使用 RegistrationData 接口作为表单字段类型
const registerForm = reactive<RegistrationData>({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
});

// 自定义校验规则：确认密码
const validatePasswordSame = (
  rule: FormItemRule,
  value: string
): boolean => {
  return value === registerForm.password;
};

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: ['input', 'blur'] },
    {
      min: 3,
      max: 20,
      message: '用户名长度应在 3 到 20 个字符之间',
      trigger: ['input', 'blur'],
    },
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: ['input', 'blur'] },
    {
      type: 'email',
      message: '请输入有效的邮箱地址',
      trigger: ['input', 'blur'],
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: ['input', 'blur'] },
    {
      min: 6,
      message: '密码长度不能少于 6 个字符',
      trigger: ['input', 'blur'],
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: ['input', 'blur'] },
    {
      validator: validatePasswordSame,
      message: '两次输入的密码不一致',
      trigger: ['input', 'blur', 'password-input'], // password-input 是为了在密码框变化时也触发此校验
    },
  ],
};

const handleRegister = async () => {
  if (isLoading.value) return;

  try {
    console.log('[RegisterPage] 开始表单验证');
    await formRef.value?.validate();
    isLoading.value = true;

    const registrationPayload: RegistrationData = {
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
    };

    console.log('[RegisterPage] 表单验证通过，准备发送注册请求:',
      { ...registrationPayload, password: '***', confirmPassword: '***' });

    await authStore.register(registrationPayload);
    console.log('[RegisterPage] 注册成功');

    message.success('注册成功！请登录您的新账户。');
    router.push({ name: 'Login' }); // 跳转到登录页

  } catch (error: any) {
    if (error && Array.isArray(error)) {
      // Naive UI 表单校验失败通常会抛出错误数组
      console.log('[RegisterPage] 注册表单校验失败:', error);
      // message.warning('请检查表单输入项是否正确。'); // 表单项会自动显示错误
    } else {
      // 其他错误，例如来自 authStore.register 的错误
      console.error('[RegisterPage] 注册失败:', error);
      console.error('[RegisterPage] 错误详情:', {
        message: error.message,
        stack: error.stack,
        name: error.name,
        response: error.response
      });
      message.error(error.message || '注册失败，请稍后重试。');
    }
  } finally {
    isLoading.value = false;
    console.log('[RegisterPage] 注册流程结束');
  }
};
</script>

<style scoped>
.register-page-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 64px - 64px - 48px);
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 450px; /* 可以比登录卡片略宽一点，因为字段更多 */
}

.register-card .n-form-item-row {
  margin-bottom: 20px; /* 调整表单项间距 */
}

.register-card .n-form-item-row:last-of-type {
  margin-bottom: 24px;
}
</style>