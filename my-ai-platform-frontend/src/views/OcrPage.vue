<template>
  <div class="ocr-page">

    <n-card title="OCR文档处理" class="ocr-card">
      <template #header-extra>
        <n-space>
          <n-button @click="resetForm" :disabled="isProcessing || isLoading">
            重置
          </n-button>
        </n-space>
      </template>

      <!-- 文件上传区域 -->
      <div class="upload-section" v-if="!currentTask || currentTask.status === 'FAILED'">
        <n-upload
          ref="uploadRef"
          :custom-request="customRequest"
          :max="1"
          :accept="acceptFileTypes"
          :disabled="isUploading || isProcessing"
          :show-file-list="false"
          @before-upload="handleBeforeUpload"
        >
          <n-upload-dragger>
            <div class="upload-trigger">
              <n-icon size="48" :depth="3">
                <document-outline />
              </n-icon>
              <div class="upload-text">
                <p>点击或拖拽文件到此区域上传</p>
                <p class="upload-hint">支持PDF、图片等文件格式</p>
              </div>
            </div>
          </n-upload-dragger>
        </n-upload>

        <!-- 上传选项 -->
        <div class="upload-options">
          <n-form
            ref="formRef"
            :model="formValue"
            label-placement="left"
            label-width="auto"
            :disabled="isUploading || isProcessing"
          >
            <n-form-item label="OCR处理选项">
              <n-space vertical>
                <n-checkbox v-model:checked="formValue.usePypdf2">
                  使用PyPDF2提取文本
                </n-checkbox>
                <n-checkbox v-model:checked="formValue.useDocling">
                  使用Docling进行OCR
                </n-checkbox>
                <n-checkbox v-model:checked="formValue.useGemini">
                  使用Gemini进行内容分析
                </n-checkbox>
                <n-checkbox v-model:checked="formValue.forceOcr">
                  强制OCR处理（即使PDF包含文本）
                </n-checkbox>
              </n-space>
            </n-form-item>

            <n-form-item label="语言">
              <n-select
                v-model:value="formValue.language"
                :options="languageOptions"
                placeholder="选择文档语言"
              />
            </n-form-item>
          </n-form>
        </div>
      </div>

      <!-- 处理中状态 -->
      <div v-if="isProcessing && uploadedFile && currentTask?.fileName" class="processing-section">
        <n-spin size="large">
          <template #description>
            <span>正在处理文档，请稍候...</span>
          </template>
        </n-spin>
        <div class="processing-info" v-if="currentTask">
          <p>任务ID: {{ currentTask.taskId }}</p>
          <p>文件名: {{ currentTask.fileName }}</p>
          <p>创建时间: {{ currentTask.createdAt }}</p>
        </div>
      </div>

      <!-- 处理结果 -->
      <div v-if="currentTask?.status === 'COMPLETED'" class="result-section">
        <div class="result-header">
          <h3>处理结果</h3>
          <n-space>
            <n-button @click="refreshResult" type="primary" :loading="isLoading">
              刷新结果
            </n-button>
            <n-button @click="resetForm">
              处理新文档
            </n-button>
          </n-space>
        </div>

        <div class="result-info">
          <p>任务ID: {{ currentTask.taskId }}</p>
          <p>文件名: {{ currentTask.fileName }}</p>
          <p>创建时间: {{ currentTask.createdAt }}</p>
          <p>完成时间: {{ currentTask.completedAt }}</p>
          <p>处理耗时: {{ processingTime }}</p>
        </div>

        <!-- 结果内容 -->
        <div class="result-content">
          <n-tabs type="line" animated>
            <!-- 文本内容标签页 -->
            <n-tab-pane name="text" tab="文本内容">
              <div v-if="resultContent?.extractedText" class="result-text">
                <n-scrollbar style="max-height: 400px">
                  <pre>{{ resultContent.extractedText }}</pre>
                </n-scrollbar>
              </div>
              <n-empty v-else description="无文本内容" />
            </n-tab-pane>

            <!-- 表格内容标签页 -->
            <n-tab-pane name="tables" tab="表格内容">
              <div v-if="resultContent?.tables && resultContent.tables.length > 0" class="result-tables">
                <div v-for="(table, index) in resultContent.tables" :key="index" class="table-item">
                  <h4>表格 {{ index + 1 }}</h4>
                  <n-data-table
                    :columns="getTableColumns(table)"
                    :data="getTableData(table)"
                    :bordered="true"
                    :single-line="false"
                  />
                </div>
              </div>
              <n-empty v-else description="无表格内容" />
            </n-tab-pane>

            <!-- Gemini分析标签页 -->
            <n-tab-pane name="analysis" tab="内容分析">
              <div v-if="resultContent?.analysis" class="result-analysis">
                <n-scrollbar style="max-height: 400px">
                  <div v-html="formattedAnalysis"></div>
                </n-scrollbar>
              </div>
              <n-empty v-else description="无内容分析" />
            </n-tab-pane>

            <!-- 原始JSON标签页 -->
            <n-tab-pane name="json" tab="原始JSON">
              <n-scrollbar style="max-height: 400px">
                <pre>{{ JSON.stringify(currentTask.result, null, 2) }}</pre>
              </n-scrollbar>
            </n-tab-pane>
          </n-tabs>
        </div>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import {
  NCard, NUpload, NUploadDragger, NIcon, NButton, NSpace,
  NSpin, NTabs, NTabPane, NEmpty, NScrollbar, NDataTable,
  NForm, NFormItem, NCheckbox, NSelect
} from 'naive-ui';
import { DocumentOutline } from '@vicons/ionicons5';
import { useOcrStore } from '../stores/ocrStore';
import { OcrTaskStatus } from '../services/ocrService';

// 状态管理
const ocrStore = useOcrStore();
const uploadRef = ref();
const formRef = ref();
const uploadedFile = ref(null); // 跟踪上传的文件

// 表单数据
const formValue = ref({
  usePypdf2: true,
  useDocling: true,
  useGemini: true,
  forceOcr: false,
  language: 'auto'
});

// 语言选项
const languageOptions = [
  { label: '自动检测', value: 'auto' },
  { label: '中文', value: 'chi_sim' },
  { label: '英文', value: 'eng' },
  { label: '日文', value: 'jpn' },
  { label: '韩文', value: 'kor' },
  { label: '中英混合', value: 'chi_sim+eng' }
];

// 接受的文件类型
const acceptFileTypes = '.pdf,.jpg,.jpeg,.png,.tiff,.tif,.bmp';

// 计算属性
const isUploading = computed(() => ocrStore.isUploading);
const isProcessing = computed(() => ocrStore.isProcessing);
const isLoading = computed(() => ocrStore.isLoading);
const currentTask = computed(() => ocrStore.currentTask);
const resultContent = computed(() => {
  if (!currentTask.value) {
    console.log('currentTask is null');
    return null;
  }

  console.log('currentTask:', JSON.stringify(currentTask.value, null, 2));

  if (!currentTask.value.result) {
    console.log('currentTask.result is null or undefined');
    // 如果任务已完成但没有结果，尝试重新获取结果
    if (currentTask.value.status === OcrTaskStatus.COMPLETED && currentTask.value.taskId) {
      console.log('任务已完成但没有结果，尝试重新获取');
      ocrStore.getTaskResult(currentTask.value.taskId).catch(e => {
        console.error('重新获取结果失败:', e);
      });
    }
    return null;
  }

  console.log('resultContent:', JSON.stringify(currentTask.value.result, null, 2));
  return currentTask.value.result;
});

// 格式化分析结果（简单的换行处理）
const formattedAnalysis = computed(() => {
  if (!resultContent.value?.analysis) return '';

  // 检查analysis是否为错误对象
  if (resultContent.value.analysis.error) {
    return `<span style="color: red;">分析失败: ${resultContent.value.analysis.error}</span>`;
  }

  // 如果是字符串，进行格式化
  if (typeof resultContent.value.analysis === 'string') {
    // 简单地将换行符转换为<br>标签，将Markdown的#标题转换为<h>标签
    return resultContent.value.analysis
      .replace(/\n/g, '<br>')
      .replace(/#{1,6}\s+(.*?)(?:\n|$)/g, '<strong>$1</strong><br>');
  }

  // 如果是对象但不是错误对象，转为JSON字符串
  return JSON.stringify(resultContent.value.analysis, null, 2)
    .replace(/\n/g, '<br>')
    .replace(/ /g, '&nbsp;');
});

// 计算处理耗时
const processingTime = computed(() => {
  if (!currentTask.value || !currentTask.value.createdAt || !currentTask.value.completedAt) {
    return '未知';
  }

  try {
    const createdAt = new Date(currentTask.value.createdAt);
    const completedAt = new Date(currentTask.value.completedAt);
    const diffMs = completedAt.getTime() - createdAt.getTime();

    // 如果时间差小于1秒，显示毫秒
    if (diffMs < 1000) {
      return `${diffMs}毫秒`;
    }

    // 否则显示秒
    const diffSec = Math.floor(diffMs / 1000);
    return `${diffSec}秒`;
  } catch (e) {
    console.error('计算处理耗时出错:', e);
    return '计算错误';
  }
});

// 自定义上传请求
const customRequest = ({ file }) => {
  if (!file) return;

  console.log('上传文件:', file);
  console.log('文件类型:', file.type);
  console.log('文件大小:', file.size);

  // 设置上传的文件
  if (file instanceof File) {
    uploadedFile.value = file;
    ocrStore.uploadFile(file, {
      usePypdf2: formValue.value.usePypdf2,
      useDocling: formValue.value.useDocling,
      useGemini: formValue.value.useGemini,
      forceOcr: formValue.value.forceOcr,
      language: formValue.value.language
    });
  } else if (file.file && file.file instanceof File) {
    // 有些UI组件可能会将文件包装在一个对象中
    uploadedFile.value = file.file;
    ocrStore.uploadFile(file.file, {
      usePypdf2: formValue.value.usePypdf2,
      useDocling: formValue.value.useDocling,
      useGemini: formValue.value.useGemini,
      forceOcr: formValue.value.forceOcr,
      language: formValue.value.language
    });
  } else {
    console.error('无效的文件对象:', file);
  }
};

// 重置表单
const resetForm = () => {
  ocrStore.reset();
  uploadedFile.value = null; // 清除上传的文件
  if (uploadRef.value) {
    uploadRef.value.clear();
  }
  formValue.value = {
    usePypdf2: true,
    useDocling: true,
    useGemini: true,
    forceOcr: false,
    language: 'auto'
  };
};

// 刷新结果
const refreshResult = async () => {
  if (currentTask.value && currentTask.value.taskId) {
    try {
      console.log('手动刷新结果，任务ID:', currentTask.value.taskId);
      await ocrStore.getTaskResult(currentTask.value.taskId);
      console.log('刷新结果成功');
    } catch (error) {
      console.error('刷新结果失败:', error);
    }
  }
};

// 获取表格列
const getTableColumns = (table) => {
  if (!table || !table.data || table.data.length === 0) return [];

  // 使用第一行数据的键作为列
  const firstRow = table.data[0];
  return Object.keys(firstRow).map(key => ({
    title: key,
    key,
    ellipsis: {
      tooltip: true
    }
  }));
};

// 获取表格数据
const getTableData = (table) => {
  if (!table || !table.data) return [];
  return table.data;
};

// 文件上传前的处理
const handleBeforeUpload = ({ file }) => {
  console.log('文件上传前:', file);
  if (file) {
    // 设置上传的文件，但此时还未开始上传
    if (file instanceof File) {
      uploadedFile.value = file;
    } else if (file.file && file.file instanceof File) {
      uploadedFile.value = file.file;
    }
  }
  return true; // 允许上传
};

// 生命周期钩子
onMounted(() => {
  // 添加调试日志
  console.log('OcrPage mounted, currentTask:', currentTask.value);
  console.log('isProcessing:', isProcessing.value);
  console.log('isUploading:', isUploading.value);
  console.log('uploadedFile:', uploadedFile.value);

  // 强制重置状态，确保页面加载时不会显示加载指示器
  ocrStore.stopPolling();
  ocrStore.reset();
  uploadedFile.value = null;

  // 不再自动开始轮询，只有在用户上传文件后才开始轮询
  console.log('页面加载完成，状态已重置');
});

onUnmounted(() => {
  // 停止轮询
  ocrStore.stopPolling();
});
</script>

<style scoped>
.ocr-page {
  padding: 20px;
}

.ocr-card {
  max-width: 1000px;
  margin: 0 auto;
}

.upload-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
}

.upload-text {
  margin-top: 16px;
  text-align: center;
}

.upload-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.upload-options {
  margin-top: 20px;
}

.processing-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.processing-info {
  margin-top: 20px;
  text-align: center;
}

.result-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-info {
  background-color: rgba(0, 0, 0, 0.02);
  padding: 10px;
  border-radius: 4px;
}

.result-content {
  margin-top: 20px;
}

.result-text pre {
  white-space: pre-wrap;
  word-wrap: break-word;
}

.table-item {
  margin-bottom: 20px;
}

.table-item h4 {
  margin-bottom: 10px;
}

/* 全局加载指示器已移除 */
</style>
