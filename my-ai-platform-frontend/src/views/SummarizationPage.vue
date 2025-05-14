<template>
  <div class="summarization-page">
    <n-page-header title="网页内容摘要" subtitle="输入一个公开网页URL，AI将为您提取内容并生成摘要。" />

    <n-card class="input-card" title="输入URL进行摘要">
      <n-form @submit.prevent="handleSummarize">
        <n-form-item label="网页URL" path="inputUrl">
          <n-input
            v-model:value="inputUrl"
            placeholder="例如：https://www.example.com/article.html"
            clearable
            :disabled="summarizationStore.isLoading"
          />
        </n-form-item>
        <n-space justify="end">
          <n-button 
            type="primary" 
            attr-type="submit" 
            :loading="summarizationStore.isLoading" 
            :disabled="!summarizationStore.canSubmit || !inputUrl.trim()"
          >
            开始摘要
          </n-button>
          <n-button @click="handleReset" :disabled="summarizationStore.isLoading">
            重置
          </n-button>
        </n-space>
      </n-form>
    </n-card>

    <n-spin :show="summarizationStore.isLoading" class="results-spin">
      <n-card v-if="summarizationStore.error" title="发生错误" class="error-card">
        <n-alert type="error" :show-icon="false">
          {{ summarizationStore.error }}
        </n-alert>
      </n-card>

      <n-card v-if="summarizationStore.hasSummaryResult && !summarizationStore.error" title="摘要结果" class="results-card">
        <n-blockquote>
          <div v-html="formattedSummary"></div>
        </n-blockquote>
        <n-p v-if="summarizationStore.lastSubmittedUrl" class="source-url">
          <strong>源URL:</strong> <a :href="summarizationStore.lastSubmittedUrl" target="_blank" rel="noopener noreferrer">{{ summarizationStore.lastSubmittedUrl }}</a>
        </n-p>
      </n-card>
      
      <n-empty 
        v-if="!summarizationStore.isLoading && !summarizationStore.hasSummaryResult && !summarizationStore.error"
        description="请输入一个公开网页URL开始生成内容摘要。"
        class="empty-state"
      />
    </n-spin>

  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import {
  NPageHeader,
  NCard,
  NForm,
  NFormItem,
  NInput,
  NButton,
  NSpace,
  NSpin,
  NAlert,
  NBlockquote,
  NEmpty,
  NP,
  useMessage // 如果需要更细致的消息提示，可以引入
} from 'naive-ui';
import { useSummarizationStore } from '../stores/summarizationStore';

const summarizationStore = useSummarizationStore();
const message = useMessage(); // 可选，用于更细致的提示

const inputUrl = ref('');

const handleSummarize = async () => {
  if (!inputUrl.value || !inputUrl.value.trim()) {
    message.error('请输入有效的URL。'); // 使用 Naive UI message 提示
    return;
  }
  // 简单的URL格式校验 (更复杂的校验建议使用库或在store/service中进行)
  try {
    new URL(inputUrl.value); // 尝试解析URL，无效则抛错
  } catch (_) {
    message.error('URL格式不正确，请输入有效的网址，例如：http://example.com');
    return;
  }

  await summarizationStore.fetchSummary(inputUrl.value);
  // 可以在这里根据 store.error 的状态给出成功或失败的 message 提示
  if (summarizationStore.error) {
    // 错误已经在store中设置，并在UI上通过n-alert显示
    // message.error(`摘要失败: ${summarizationStore.error}`);
  } else if (summarizationStore.hasSummaryResult) {
    message.success('摘要生成成功！');
  }
};

const handleReset = () => {
  summarizationStore.resetState();
  inputUrl.value = '';
  message.info('输入和结果已重置。');
};

// 将摘要文本中的换行符转换成 <br> 标签，以便在 v-html 中正确显示换行
const formattedSummary = computed(() => {
  if (summarizationStore.summaryResult) {
    // 将 \n 替换为 <br /> 以便在 v-html 中正确显示换行
    // 同时，为了安全起见，如果摘要内容可能包含HTML特殊字符，应先进行转义，然后再替换换行符
    // 但此处假设AI返回的摘要是纯文本或安全的HTML片段
    return summarizationStore.summaryResult.replace(/\n/g, '<br />');
  }
  return '';
});

</script>

<style scoped>
.summarization-page {
  padding: 20px;
}

.input-card {
  margin-top: 20px;
  margin-bottom: 24px;
}

.results-spin {
  margin-top: 20px;
}

.error-card,
.results-card {
  margin-top: 20px;
}

.results-card .n-blockquote {
  white-space: pre-wrap; /* 保留换行和空格，自动换行 */
  word-wrap: break-word; /* 超长单词断行 */
  background-color: #f7f7f7;
  padding: 15px;
  border-radius: 4px;
  border: 1px solid #efefef;
}

.empty-state {
  margin-top: 40px;
  min-height: 200px; /* 给空状态一些高度 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.source-url {
  margin-top: 15px;
  font-size: 0.9em;
  color: #555;
}
.source-url a {
  color: #18a058; /* Naive UI 主题色 */
  text-decoration: none;
}
.source-url a:hover {
  text-decoration: underline;
}
</style> 