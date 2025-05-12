<template>
  <div class="prompts-container">
    <!-- 页面标题和操作按钮 -->
    <n-page-header style="margin-bottom: 20px;">
      <template #title>
        Prompt 管理
      </template>
      <template #extra>
        <n-button type="primary" @click="handleNewPrompt">
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
          <n-button size="small" @click="handleNewPrompt">
            立即创建
          </n-button>
        </template>
      </n-empty>

    </n-spin>

    <!-- TODO: 新建/编辑 Prompt 的模态框 -->

  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted, computed } from 'vue';
import { usePromptStore } from '../stores/promptStore';
import { NDataTable, NButton, NIcon, NSpace, NPageHeader, NSpin, NAlert, NEmpty, useDialog, useMessage } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { AddOutline as AddIcon, CreateOutline as EditIcon, TrashOutline as DeleteIcon } from '@vicons/ionicons5';
import type { Prompt } from '../services/promptService';

const promptStore = usePromptStore();
const dialog = useDialog();
const message = useMessage();

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
      return row.createdAt ? new Date(row.createdAt).toLocaleString() : '-';
    }
  },
  {
    title: '更新时间',
    key: 'updatedAt',
    render(row) {
      return row.updatedAt ? new Date(row.updatedAt).toLocaleString() : '-';
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
                onClick: () => handleEdit(row)
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
const handleNewPrompt = () => {
  // TODO: 实现打开新建模态框或导航到新建页面
  message.info('新建 Prompt 功能待实现');
};

const handleEdit = (prompt: Prompt) => {
  // TODO: 实现打开编辑模态框或导航到编辑页面
  message.info(`编辑 Prompt 功能待实现 (ID: ${prompt.id})`);
};

const handleDelete = (prompt: Prompt) => {
  // TODO: 实现删除确认和调用 store action
  dialog.warning({
    title: '确认删除',
    content: `您确定要删除 Prompt "${prompt.title}" 吗？此操作无法撤销。`,
    positiveText: '确认',
    negativeText: '取消',
    onPositiveClick: async () => {
      message.info(`删除 Prompt 功能待实现 (ID: ${prompt.id})`);
      // try {
      //   await promptStore.removePrompt(prompt.id);
      //   message.success('Prompt 删除成功！');
      // } catch (error: any) {
      //   message.error(error.message || '删除 Prompt 失败');
      // }
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
</style> 