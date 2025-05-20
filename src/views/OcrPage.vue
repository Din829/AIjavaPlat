<template>
  <div class="ocr-page">
    <n-card title="OCR文档处理" class="ocr-card">
      <template #header-extra>
        <n-space>
          <n-button @click="resetForm" :disabled="isProcessing">
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
        >
          <n-upload-dragger>
            <div class="upload-trigger">
              <n-icon size="48" :depth="3">
                <document-add-outline />
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
      <div v-if="isProcessing || currentTask?.status === 'PROCESSING'" class="processing-section">
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
            <n-button @click="resetForm" type="primary">
              处理新文档
            </n-button>
          </n-space>
        </div>
        
        <div class="result-info">
          <p>任务ID: {{ currentTask.taskId }}</p>
          <p>文件名: {{ currentTask.fileName }}</p>
          <p>创建时间: {{ currentTask.createdAt }}</p>
          <p>完成时间: {{ currentTask.completedAt }}</p>
        </div>
        
        <!-- 结果内容 -->
        <div class="result-content">
          <n-tabs type="line" animated>
            <!-- 文本内容标签页 -->
            <n-tab-pane name="text" tab="文本内容">
              <div v-if="resultContent?.text" class="result-text">
                <n-scrollbar style="max-height: 400px">
                  <pre>{{ resultContent.text }}</pre>
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
import { DocumentAddOutline } from '@vicons/ionicons5';
import { useOcrStore } from '@/stores/ocrStore';
import { OcrTaskStatus } from '@/services/ocrService';
import { marked } from 'marked';

// 状态管理
const ocrStore = useOcrStore();
const uploadRef = ref();
const formRef = ref();

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
const currentTask = computed(() => ocrStore.currentTask);
const resultContent = computed(() => {
  if (!currentTask.value || !currentTask.value.result) return null;
  return currentTask.value.result;
});

// 格式化分析结果（将Markdown转换为HTML）
const formattedAnalysis = computed(() => {
  if (!resultContent.value?.analysis) return '';
  return marked(resultContent.value.analysis);
});

// 自定义上传请求
const customRequest = ({ file }) => {
  if (!file) return;
  
  ocrStore.uploadFile(file, {
    usePypdf2: formValue.value.usePypdf2,
    useDocling: formValue.value.useDocling,
    useGemini: formValue.value.useGemini,
    forceOcr: formValue.value.forceOcr,
    language: formValue.value.language
  });
};

// 重置表单
const resetForm = () => {
  ocrStore.reset();
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

// 生命周期钩子
onMounted(() => {
  // 如果有正在处理的任务，开始轮询
  if (currentTask.value && 
      (currentTask.value.status === OcrTaskStatus.PENDING || 
       currentTask.value.status === OcrTaskStatus.PROCESSING)) {
    ocrStore.startPolling(currentTask.value.taskId);
  }
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
</style>
