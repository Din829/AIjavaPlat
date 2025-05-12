<template>
  <div class="tokens-container">
    <n-card title="API Token管理" class="tokens-card">
      <!-- 页面标题和说明 -->
      <div class="page-header">
        <h2>API Token管理</h2>
        <p class="description">
          在这里管理您的AI服务API Token。这些Token用于驱动平台的AI功能，如网页内容摘要。
          <br>
          <strong>注意：</strong> 您的Token将被安全加密存储，不会被平台管理员或其他用户访问。
        </p>
      </div>

      <!-- 操作按钮区域 -->
      <div class="actions">
        <n-button
          type="primary"
          @click="showCreateTokenModal = true"
          :disabled="creatingToken"
        >
          <!-- 移除了图标 -->
          添加新Token
        </n-button>

        <n-button
          @click="refreshTokens"
          :disabled="loading"
        >
          <!-- 移除了图标 -->
          刷新
        </n-button>
      </div>

      <!-- Token列表 -->
      <div class="token-list">
        <n-spin :show="loading">
          <!-- 无数据时显示的内容 -->
          <n-empty
            v-if="!loading && sortedTokens.length === 0"
            description="您还没有添加任何API Token"
          >
            <template #extra>
              <n-button
                type="primary"
                @click="showCreateTokenModal = true"
              >
                添加第一个Token
              </n-button>
            </template>
          </n-empty>

          <!-- Token列表表格 -->
          <n-data-table
            v-else
            :columns="columns"
            :data="sortedTokens"
            :bordered="false"
            :single-line="false"
            :row-key="row => row.id"
          />
        </n-spin>
      </div>
    </n-card>

    <!-- 创建Token的模态框 -->
    <n-modal
      v-model:show="showCreateTokenModal"
      preset="card"
      title="添加新的API Token"
      style="width: 500px"
      :mask-closable="!creatingToken"
      :close-on-esc="!creatingToken"
    >
      <n-form
        ref="createFormRef"
        :model="createTokenForm"
        :rules="createTokenRules"
        label-placement="left"
        label-width="auto"
        require-mark-placement="right-hanging"
      >
        <!-- 提供商选择 -->
        <n-form-item label="提供商" path="provider">
          <n-select
            v-model:value="createTokenForm.provider"
            :options="providerOptions"
            placeholder="请选择AI服务提供商"
            :disabled="creatingToken"
          />
        </n-form-item>

        <!-- Token名称/描述 -->
        <n-form-item label="名称/描述" path="name">
          <n-input
            v-model:value="createTokenForm.name"
            placeholder="为您的Token添加描述，如'摘要服务'"
            :disabled="creatingToken"
          />
        </n-form-item>

        <!-- Token值 -->
        <n-form-item label="Token值" path="tokenValue">
          <n-input
            v-model:value="createTokenForm.tokenValue"
            type="password"
            show-password-on="click"
            placeholder="请输入您的API Token"
            :disabled="creatingToken"
          />
        </n-form-item>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <n-button
            @click="showCreateTokenModal = false"
            :disabled="creatingToken"
          >
            取消
          </n-button>
          <n-button
            type="primary"
            @click="handleCreateToken"
            :loading="creatingToken"
            :disabled="creatingToken"
          >
            {{ creatingToken ? '创建中...' : '创建' }}
          </n-button>
        </div>
      </n-form>
    </n-modal>

    <!-- 删除确认对话框 -->
    <n-modal
      v-model:show="showDeleteConfirm"
      preset="dialog"
      title="确认删除"
      content="您确定要删除这个API Token吗？此操作不可撤销。"
      positive-text="删除"
      negative-text="取消"
      type="warning"
      @positive-click="confirmDelete"
      @negative-click="cancelDelete"
      :loading="!!deletingTokenId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, h } from 'vue';
import { useTokenStore } from '../stores/tokenStore';
import { storeToRefs } from 'pinia';
import {
  TokenProvider,
  getProviderOptions,
  getProviderDisplayName,
  maskTokenValue,
  type CreateTokenRequest,
  type Token
} from '../services/tokenService';
import {
  NCard,
  NButton,
  NDataTable,
  NEmpty,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NSelect,
  NSpin,
  type FormInst,
  type FormRules
} from 'naive-ui';

// 获取Token状态管理store
const tokenStore = useTokenStore();

// 使用storeToRefs保持响应性
const { loading, creatingToken, deletingTokenId } = storeToRefs(tokenStore);

// 计算属性：按创建时间排序的Token列表
const sortedTokens = computed(() => tokenStore.sortedTokens);

// 提供商选项
const providerOptions = getProviderOptions();

// 创建Token表单引用
const createFormRef = ref<FormInst | null>(null);

// 创建Token表单数据
const createTokenForm = reactive<CreateTokenRequest>({
  provider: TokenProvider.OPENAI, // 默认选择OpenAI
  name: '',
  tokenValue: ''
});

// 创建Token表单验证规则
const createTokenRules: FormRules = {
  provider: [
    { required: true, message: '请选择提供商', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入名称/描述', trigger: 'blur' },
    { min: 2, max: 50, message: '名称长度应在2-50个字符之间', trigger: 'blur' }
  ],
  tokenValue: [
    { required: true, message: '请输入Token值', trigger: 'blur' },
    { min: 8, message: 'Token长度不能小于8个字符', trigger: 'blur' }
  ]
};

// 模态框显示状态
const showCreateTokenModal = ref(false);
const showDeleteConfirm = ref(false);

// 待删除的Token ID
const tokenToDelete = ref<number | null>(null);

// 表格列定义
const columns = [
  {
    title: '提供商',
    key: 'provider',
    render(row: Token) {
      return getProviderDisplayName(row.provider);
    }
  },
  {
    title: '名称/描述',
    key: 'name'
  },
  {
    title: 'Token值',
    key: 'tokenValue',
    render(row: Token) {
      return maskTokenValue(row.tokenValue);
    }
  },
  {
    title: '创建时间',
    key: 'createdAt',
    render(row: Token) {
      return new Date(row.createdAt).toLocaleString();
    }
  },
  {
    title: '操作',
    key: 'actions',
    render(row: Token) {
      return h(
        NButton,
        {
          type: 'error',
          size: 'small',
          onClick: () => handleDeleteToken(row.id),
          loading: deletingTokenId.value === row.id,
          disabled: !!deletingTokenId.value
        },
        {
          default: () => '删除'
        }
      );
    }
  }
];

// 组件挂载时加载Token列表
onMounted(async () => {
  await refreshTokens();
});

// 刷新Token列表
async function refreshTokens() {
  await tokenStore.fetchTokens();
}

// 处理创建Token
async function handleCreateToken() {
  createFormRef.value?.validate(async (errors) => {
    if (errors) {
      return;
    }

    // 创建Token
    const newToken = await tokenStore.addToken({
      provider: createTokenForm.provider,
      name: createTokenForm.name,
      tokenValue: createTokenForm.tokenValue
    });

    if (newToken) {
      // 创建成功，重置表单并关闭模态框
      resetCreateForm();
      showCreateTokenModal.value = false;
    }
  });
}

// 重置创建Token表单
function resetCreateForm() {
  createTokenForm.provider = TokenProvider.OPENAI;
  createTokenForm.name = '';
  createTokenForm.tokenValue = '';
  createFormRef.value?.restoreValidation();
}

// 处理删除Token
function handleDeleteToken(id: number) {
  tokenToDelete.value = id;
  showDeleteConfirm.value = true;
}

// 确认删除Token
async function confirmDelete() {
  if (tokenToDelete.value !== null) {
    const success = await tokenStore.removeToken(tokenToDelete.value);
    if (success) {
      tokenToDelete.value = null;
    }
  }
}

// 取消删除Token
function cancelDelete() {
  tokenToDelete.value = null;
}
</script>

<style scoped>
.tokens-container {
  padding: 20px;
}

.tokens-card {
  width: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin-top: 0;
  margin-bottom: 8px;
  color: #1890ff;
}

.description {
  color: #666;
  line-height: 1.5;
}

.actions {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
  margin-bottom: 24px;
}

.token-list {
  min-height: 200px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>
