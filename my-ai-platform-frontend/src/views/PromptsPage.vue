<template>
  <div class="prompts-container">
    <!-- 页面标题和操作按钮 -->
    <n-page-header style="margin-bottom: 20px;">
      <template #title>
        Prompt 管理
      </template>
      <template #extra>
        <n-button type="primary" @click="openNewPromptModal">
          <template #icon>
            <n-icon :component="AddIcon" />
          </template>
          新建 Prompt
        </n-button>
      </template>
    </n-page-header>

    <!-- 加载状态 -->
    <n-spin :show="promptStore.loading">
      <!-- 错误提示 -->
      <n-alert v-if="promptStore.error" title="加载错误" type="error" closable style="margin-bottom: 20px;">
        {{ promptStore.error }}
      </n-alert>

      <!-- Prompt 数据表格 -->
      <n-data-table
        :columns="columns"
        :data="promptStore.prompts"
        :pagination="false" 
        :bordered="false"
      />
      <!-- 如果列表为空且没有加载错误，显示提示 -->
      <n-empty v-if="!promptStore.loading && !promptStore.error && promptStore.prompts.length === 0" description="您还没有创建任何 Prompt。" style="margin-top: 40px;">
        <template #extra>
          <n-button size="small" @click="openNewPromptModal">
            立即创建
          </n-button>
        </template>
      </n-empty>

    </n-spin>

    <!-- 新建 Prompt 的模态框 -->
    <n-modal
      v-model:show="showFormModal"
      preset="card"
      :title="modalTitle"
      style="width: 600px;"
      :mask-closable="false"
      :closable="true"
      @after-leave="formRef?.restoreValidation()" 
    >
      <n-card :bordered="false" size="huge" role="dialog" aria-modal="true">
        <n-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-placement="top"
        >
          <n-form-item label="标题" path="title">
            <n-input v-model:value="formData.title" placeholder="请输入 Prompt 标题" />
          </n-form-item>
          <n-form-item label="内容" path="content">
            <n-input
              v-model:value="formData.content"
              type="textarea"
              placeholder="请输入 Prompt 具体内容"
              :autosize="{ minRows: 5, maxRows: 15 }"
            />
          </n-form-item>
          <n-form-item label="分类 (可选)" path="category">
            <n-input v-model:value="formData.category" placeholder="例如：开发、文案、测试" />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-space justify="end">
            <n-button @click="handleModalClose" :disabled="isSubmitting">取消</n-button>
            <n-button type="primary" @click="handleFormSubmit" :loading="isSubmitting">{{ submitButtonText }}</n-button>
          </n-space>
        </template>
      </n-card>
    </n-modal>

  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted, computed } from 'vue';
import { usePromptStore } from '../stores/promptStore';
import { NDataTable, NButton, NIcon, NSpace, NPageHeader, NSpin, NAlert, NEmpty, useDialog, useMessage, NModal, NCard, NForm, NFormItem, NInput } from 'naive-ui';
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui';
import { AddOutline as AddIcon, CreateOutline as EditIcon, TrashOutline as DeleteIcon } from '@vicons/ionicons5';
import type { Prompt, PromptData } from '../services/promptService';

const promptStore = usePromptStore();
const dialog = useDialog();
const message = useMessage();

// --- 表单与模态框相关的响应式状态 ---
const showFormModal = ref(false); // 控制模态框显示/隐藏
const formRef = ref<FormInst | null>(null); // 表单实例引用
const formData = ref<PromptData>({ // 表单数据 (用于新建和编辑)
  title: '',
  content: '',
  category: ''
});
const editingPromptId = ref<number | string | null>(null); // 当前编辑的Prompt ID，null表示新建模式
const isSubmitting = ref(false); // 控制表单提交操作的加载状态

// --- 计算属性，用于动态改变模态框标题和提交按钮文本 ---
const modalTitle = computed(() => {
  return editingPromptId.value ? '编辑 Prompt' : '新建 Prompt';
});

const submitButtonText = computed(() => {
  return editingPromptId.value ? '更新' : '创建';
});

// --- 实用工具函数 ---
const formatDate = (dateString: string | null | undefined): string => {
  if (!dateString) return '-';
  try {
    const date = new Date(dateString);
    // 格式化为 YYYY-MM-DD HH:mm
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  } catch (e) {
    return dateString; // 如果转换失败，返回原始字符串
  }
};

// --- 表单校验规则 (保持不变) ---
const formRules: FormRules = {
  title: [
    { required: true, message: '请输入 Prompt 标题', trigger: ['input', 'blur'] }
  ],
  content: [
    { required: true, message: '请输入 Prompt 内容', trigger: ['input', 'blur'] }
  ],
  // category 不是必填项
};

// --- 表格列定义 ---
const columns = computed<DataTableColumns<Prompt>>(() => [
  {
    title: '标题',
    key: 'title'
  },
  {
    title: '分类',
    key: 'category',
    render(row) {
      return row.category || '-'; // 如果分类为空，显示 '-'
    }
  },
  {
    title: '创建时间',
    key: 'createdAt',
    render(row) {
      return formatDate(row.createdAt);
    }
  },
  {
    title: '更新时间',
    key: 'updatedAt',
    render(row) {
      return formatDate(row.updatedAt);
    }
  },
  {
    title: '操作',
    key: 'actions',
    render(row) {
      return h(
        NSpace,
        {},
        {
          default: () => [
            h(
              NButton,
              {
                size: 'small',
                type: 'info',
                text: true,
                onClick: () => openEditPromptModal(row)
              },
              { default: () => '编辑', icon: () => h(NIcon, { component: EditIcon }) }
            ),
            h(
              NButton,
              {
                size: 'small',
                type: 'error',
                text: true,
                onClick: () => handleDelete(row)
              },
              { default: () => '删除', icon: () => h(NIcon, { component: DeleteIcon }) }
            )
          ]
        }
      );
    }
  }
]);

// --- 事件处理 ---
const openNewPromptModal = () => {
  editingPromptId.value = null; // 清除编辑ID，确保是新建模式
  formData.value = { // 重置表单数据
    title: '',
    content: '',
    category: ''
  };
  isSubmitting.value = false; // 重置提交状态
  formRef.value?.restoreValidation(); // 清除上次的校验信息
  showFormModal.value = true; // 显示模态框
};

const openEditPromptModal = (prompt: Prompt) => {
  editingPromptId.value = prompt.id;
  formData.value = { // 用当前行的数据填充表单
    title: prompt.title,
    content: prompt.content,
    category: prompt.category || '' // category 可能为 null
  };
  isSubmitting.value = false;
  formRef.value?.restoreValidation();
  showFormModal.value = true;
};

const handleModalClose = () => {
  showFormModal.value = false;
  editingPromptId.value = null; // 关闭时也重置编辑ID
};

const handleFormSubmit = async () => {
  try {
    await formRef.value?.validate();
    isSubmitting.value = true;

    if (editingPromptId.value) {
      // 编辑模式
      await promptStore.editPrompt(editingPromptId.value, formData.value);
      message.success('Prompt 更新成功！');
    } else {
      // 新建模式
      await promptStore.addPrompt(formData.value);
      message.success('Prompt 创建成功！');
    }
    showFormModal.value = false; // 关闭模态框
    editingPromptId.value = null; // 重置编辑ID
    // 列表会自动刷新，因为 store 的 addPrompt/editPrompt action 会调用 fetchPrompts
  } catch (errors) {
    if (Array.isArray(errors)) {
      console.error('表单校验失败:', errors);
    } else {
      console.error('操作失败:', errors);
      message.error((errors as Error).message || '操作失败，请重试');
    }
  } finally {
    isSubmitting.value = false;
  }
};

const handleDelete = (prompt: Prompt) => {
  dialog.warning({
    title: '确认删除',
    content: `您确定要删除 Prompt "${prompt.title}" 吗？此操作无法撤销。`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await promptStore.removePrompt(prompt.id);
        message.success('Prompt 删除成功！');
      } catch (error: any) {
        console.error('删除 Prompt 失败:', error);
        message.error(error.message || '删除 Prompt 失败，请重试');
      }
    }
  });
};

// --- 生命周期钩子 ---
onMounted(() => {
  // 页面加载时获取 Prompt 列表
  promptStore.fetchPrompts();
});

</script>

<style scoped>
.prompts-container {
  /* 可以添加一些容器样式 */
}

/* 确保模态框内的文本域可以调整大小 */
:deep(.n-input__textarea-el) {
  resize: vertical;
}
</style> 