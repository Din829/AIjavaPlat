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
          :custom-request="batchCustomRequest"
          :max="50"
          multiple
          :accept="acceptFileTypes"
          :disabled="isUploading || isProcessing"
          :show-file-list="true"
          @before-upload="handleBeforeUpload"
        >
          <n-upload-dragger>
            <div class="upload-trigger">
              <n-icon size="48" :depth="3">
                <document-outline />
              </n-icon>
              <div class="upload-text">
                <p>点击或拖拽文件到此区域批量上传</p>
                <p class="upload-hint">支持PDF、图片、Excel、Word、文本、CSV等文件格式</p>
                <p class="upload-hint">最多可同时上传50个文件，支持批量处理</p>
              </div>
            </div>
          </n-upload-dragger>
        </n-upload>

        <!-- 文件管理区域 -->
        <div v-if="selectedFiles.length > 0" class="file-management">
          <div class="file-management-header">
            <h4>已选择的文件 ({{ selectedFiles.length }})</h4>
            <n-space>
              <n-button @click="addMoreFiles" type="primary" ghost size="small">
                <template #icon>
                  <n-icon><document-outline /></n-icon>
                </template>
                添加更多文件
              </n-button>
              <n-button @click="clearAllFiles" type="error" ghost size="small">
                清空所有文件
              </n-button>
              <n-button @click="startBatchProcessing" type="primary" size="small" :disabled="selectedFiles.length === 0">
                开始批量处理
              </n-button>
            </n-space>
          </div>

          <div class="file-list">
            <div
              v-for="(file, index) in selectedFiles"
              :key="index"
              class="file-item"
            >
              <div class="file-info">
                <n-icon class="file-icon"><document-outline /></n-icon>
                <div class="file-details">
                  <div class="file-name">{{ file.name }}</div>
                  <div class="file-meta">{{ formatFileSize(file.size) }} • {{ getFileType(file.name) }}</div>
                </div>
              </div>
              <n-button @click="removeFile(index)" type="error" ghost size="tiny">
                <template #icon>
                  <n-icon>×</n-icon>
                </template>
              </n-button>
            </div>
          </div>
        </div>

        <!-- 隐藏的文件输入 -->
        <input
          ref="fileInputRef"
          type="file"
          multiple
          :accept="acceptFileTypes"
          @change="handleFileInputChange"
          style="display: none;"
        />

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
                <!-- Docling选项暂时隐藏，因为功能有限 -->
                <!-- <n-checkbox v-model:checked="formValue.useDocling">
                  使用Docling进行OCR
                </n-checkbox> -->
                <n-checkbox v-model:checked="formValue.useGemini">
                  使用Gemini进行内容分析
                </n-checkbox>
                <n-checkbox v-model:checked="formValue.useVisionOcr">
                  使用Gemini Vision OCR（适合扫描PDF和图像）
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

            <n-form-item label="Gemini模型" v-if="formValue.useGemini || formValue.useVisionOcr">
              <n-select
                v-model:value="formValue.geminiModel"
                :options="geminiModelOptions"
                placeholder="选择Gemini模型"
              />
              <template #feedback>
                <div class="model-description">
                  {{ getModelDescription(formValue.geminiModel) }}
                </div>
              </template>
            </n-form-item>
          </n-form>
        </div>

      </div>

      <!-- 批量处理状态显示 -->
      <div v-if="isBatchMode && batchTasks.length > 0" class="batch-processing-section">
        <div class="batch-header">
          <h3>📚 批量处理进度</h3>
          <p>正在处理 {{ batchTasks.length }} 个文件</p>
        </div>

        <div class="batch-progress">
          <div class="batch-summary">
            <n-space>
              <n-tag type="info">总计: {{ batchTasks.length }}</n-tag>
              <n-tag type="warning">处理中: {{ getBatchStatusCount('PENDING') + getBatchStatusCount('PROCESSING') }}</n-tag>
              <n-tag type="success">已完成: {{ getBatchStatusCount('COMPLETED') }}</n-tag>
              <n-tag type="error">失败: {{ getBatchStatusCount('FAILED') }}</n-tag>
            </n-space>
          </div>

          <div class="batch-tasks">
            <div
              v-for="task in batchTasks"
              :key="task.taskId || task.fileName"
              class="batch-task-item"
              :class="[task.status?.toLowerCase(), { 'selected': selectedTaskId === task.taskId }]"
              @click="selectTask(task)"
            >
              <div class="task-info">
                <div class="task-name">{{ task.fileName }}</div>
                <div class="task-status">{{ getTaskStatusText(task.status) }}</div>
              </div>
              <div class="task-actions">
                <n-button
                  v-if="task.status === 'COMPLETED'"
                  @click.stop="viewTaskResult(task)"
                  type="primary"
                  ghost
                  size="tiny"
                >
                  查看结果
                </n-button>
                <div class="task-indicator">
                  <n-spin v-if="task.status === 'PENDING' || task.status === 'PROCESSING'" size="small" />
                  <span v-else-if="task.status === 'COMPLETED'" class="status-icon success">✓</span>
                  <span v-else-if="task.status === 'FAILED'" class="status-icon error">✗</span>
                  <span v-else class="status-icon pending">○</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 处理中状态 - 智能进度版 -->
      <div v-if="showProcessingIndicator && !isBatchMode" class="processing-section">
        <n-spin size="large">
          <template #description>
            <div class="processing-text">
              <h3>🔄 正在处理文档</h3>
              <p>{{ getProcessingMessage() }}</p>
            </div>
          </template>
        </n-spin>

        <!-- 任务信息 -->
        <div class="processing-info" v-if="currentTask">
          <p><strong>文件名:</strong> {{ currentTask.originalFilename }}</p>
          <p><strong>模型:</strong> {{ formValue.geminiModel === 'gemini-2.5-flash-preview-05-20' ? 'Gemini 2.5 Flash (快速)' : 'Gemini 2.5 Pro (高质量)' }}</p>
          <p><strong>状态:</strong> {{ getDetailedStatus() }}</p>
        </div>

        <!-- 现代化处理进度 -->
        <div class="progress-container" v-if="showProcessingIndicator">
          <div class="progress-header">
            <h4>处理进度</h4>
            <div class="progress-summary">{{ getProgressSummary() }}</div>
          </div>

          <div class="progress-steps">
            <div
              v-for="(step, index) in progressSteps"
              :key="index"
              :class="['progress-step', step.status]"
            >
              <div class="step-left">
                <div class="step-icon">{{ step.icon }}</div>
                <div class="step-info">
                  <div class="step-title">{{ step.message }}</div>
                  <div v-if="step.detail" class="step-subtitle">{{ step.detail }}</div>
                </div>
              </div>
              <div class="step-right">
                <div class="step-status">
                  <span v-if="step.status === 'completed'" class="status-completed">✓</span>
                  <span v-else-if="step.status === 'active'" class="status-active">
                    <n-spin size="small" />
                  </span>
                  <span v-else class="status-pending">○</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 批量处理结果 -->
      <div v-if="isBatchMode && selectedTaskId && getSelectedTaskResult()" class="result-section">
        <div class="result-header">
          <h3>处理结果 - {{ getSelectedTask()?.fileName }}</h3>
          <n-space>
            <n-select
              v-model:value="selectedTaskId"
              :options="getCompletedTaskOptions()"
              placeholder="选择要查看的文件"
              style="width: 300px;"
            />
            <n-button @click="refreshBatchTaskResult" type="primary" :loading="isLoading">
              刷新结果
            </n-button>
            <n-button @click="resetForm">
              处理新文档
            </n-button>
          </n-space>
        </div>

        <div class="result-info">
          <p>任务ID: {{ selectedTaskId }}</p>
          <p>文件名: {{ getSelectedTask()?.fileName }}</p>
          <p>创建时间: {{ getSelectedTask()?.createdAt }}</p>
          <p>完成时间: {{ getSelectedTaskResult()?.completedAt }}</p>
          <p>处理耗时: {{ calculateProcessingTime(getSelectedTask(), getSelectedTaskResult()) }}</p>
        </div>

        <!-- 结果内容 -->
        <div class="result-content">
          <n-tabs type="line" animated>
            <!-- 文本内容标签页 -->
            <n-tab-pane name="text" tab="文本内容">
              <div v-if="getSelectedTaskResult()?.result?.extractedText" class="result-text">
                <n-scrollbar style="max-height: 400px">
                  <div class="rich-text-content">
                    <RichTextDisplay
                      :text="getSelectedTaskResult().result.extractedText"
                      :images="getSelectedTaskResult()?.result?.images || []"
                    />
                  </div>
                </n-scrollbar>
              </div>
              <n-empty v-else description="无文本内容" />
            </n-tab-pane>

            <!-- Gemini分析标签页 -->
            <n-tab-pane name="analysis" tab="内容分析">
              <div v-if="getSelectedTaskResult()?.result?.analysis" class="result-analysis">
                <n-scrollbar style="max-height: 400px">
                  <div v-html="formatAnalysis(getSelectedTaskResult().result.analysis)"></div>
                </n-scrollbar>
              </div>
              <n-empty v-else description="无内容分析" />
            </n-tab-pane>

            <!-- 图像内容标签页 -->
            <n-tab-pane name="images" tab="提取图像">
              <div v-if="getSelectedTaskResult()?.result?.images && getSelectedTaskResult().result.images.length > 0" class="result-images">
                <n-scrollbar style="max-height: 400px">
                  <div class="images-grid">
                    <div
                      v-for="image in getSelectedTaskResult().result.images"
                      :key="image.image_id"
                      class="image-item"
                    >
                      <div class="image-header">
                        <h4>{{ image.description || image.image_id }}</h4>
                        <n-tag size="small" type="info">第{{ image.page_number }}页</n-tag>
                      </div>
                      <div class="image-content">
                        <img
                          :src="`data:${image.mime_type};base64,${image.data}`"
                          :alt="image.description || image.image_id"
                          class="extracted-image"
                          @click="previewImage(image)"
                        />
                      </div>
                      <div class="image-actions">
                        <n-button size="small" @click="downloadImage(image)">
                          下载图像
                        </n-button>
                      </div>
                    </div>
                  </div>
                </n-scrollbar>
              </div>
              <n-empty v-else description="未发现图像内容" />
            </n-tab-pane>
          </n-tabs>
        </div>
      </div>

      <!-- 单文件处理结果 -->
      <div v-else-if="!isBatchMode && currentTask?.status === 'COMPLETED'" class="result-section">
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
          <p>文件名: {{ currentTask.originalFilename }}</p>
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
                  <div class="rich-text-content">
                    <RichTextDisplay
                      :text="resultContent.extractedText"
                      :images="resultContent?.images || []"
                    />
                  </div>
                </n-scrollbar>
              </div>
              <n-empty v-else description="无文本内容" />
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

            <!-- 图像内容标签页 -->
            <n-tab-pane name="images" tab="提取图像">
              <div v-if="resultContent?.images && resultContent.images.length > 0" class="result-images">
                <n-scrollbar style="max-height: 400px">
                  <div class="images-grid">
                    <div
                      v-for="image in resultContent.images"
                      :key="image.image_id"
                      class="image-item"
                    >
                      <div class="image-header">
                        <h4>{{ image.description || image.image_id }}</h4>
                        <n-tag size="small" type="info">第{{ image.page_number }}页</n-tag>
                      </div>
                      <div class="image-content">
                        <img
                          :src="`data:${image.mime_type};base64,${image.data}`"
                          :alt="image.description || image.image_id"
                          class="extracted-image"
                          @click="previewImage(image)"
                        />
                      </div>
                      <div class="image-actions">
                        <n-button size="small" @click="downloadImage(image)">
                          下载图像
                        </n-button>
                      </div>
                    </div>
                  </div>
                </n-scrollbar>
              </div>
              <n-empty v-else description="未发现图像内容" />
            </n-tab-pane>
          </n-tabs>
        </div>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import {
  NCard, NUpload, NUploadDragger, NIcon, NButton, NSpace,
  NSpin, NTabs, NTabPane, NEmpty, NScrollbar,
  NForm, NFormItem, NCheckbox, NSelect, NTag
} from 'naive-ui';
import { DocumentOutline } from '@vicons/ionicons5';
import { useOcrStore } from '../stores/ocrStore';
import { OcrTaskStatus } from '../services/ocrService';
import RichTextDisplay from '../components/RichTextDisplay.vue';

// 状态管理
const ocrStore = useOcrStore();
const uploadRef = ref();
const formRef = ref();
const uploadedFile = ref(null); // 跟踪上传的文件

// 批量处理相关状态
const batchTasks = ref([]); // 批量任务列表
const batchId = ref(null); // 批量任务ID
const isBatchMode = ref(false); // 是否为批量模式
const selectedFiles = ref([]); // 已选择的文件列表
const fileInputRef = ref(); // 文件输入引用
const selectedTaskId = ref(null); // 当前选中查看的任务ID
const batchTaskResults = ref(new Map()); // 批量任务结果缓存

// 表单数据
const formValue = ref({
  usePypdf2: true,
  useDocling: false,  // 隐藏Docling选项，设为false
  useGemini: true,
  useVisionOcr: false,  // 新增Vision OCR选项，默认关闭
  forceOcr: false,
  language: 'auto',
  geminiModel: 'gemini-2.5-flash-preview-05-20'  // 默认使用最快的模型
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

// Gemini模型选项
const geminiModelOptions = [
  { label: 'Gemini 2.5 Flash Preview 05-20 (快速)', value: 'gemini-2.5-flash-preview-05-20' },
  { label: 'Gemini 2.5 Pro Preview 05-06 (最佳OCR质量)', value: 'gemini-2.5-pro-preview-05-06' }
];

// 获取模型描述
const getModelDescription = (modelValue: string) => {
  switch (modelValue) {
    case 'gemini-2.5-flash-preview-05-20':
      return '最新的快速模型，在保持良好质量的同时大幅提升处理速度，适合快速文档分析';
    case 'gemini-2.5-pro-preview-05-06':
      return '最高的OCR识别质量，专门优化用于扫描PDF和图像文字识别，精度最高但处理时间较长';
    default:
      return '';
  }
};

// 接受的文件类型
const acceptFileTypes = '.pdf,.jpg,.jpeg,.png,.tiff,.tif,.bmp,.xlsx,.xls,.xlsm,.docx,.doc,.txt,.md,.rtf,.csv,.tsv';

// 计算属性
const isUploading = computed(() => ocrStore.isUploading);
const isProcessing = computed(() => ocrStore.isProcessing);
const isLoading = computed(() => ocrStore.isLoading);
const currentTask = computed(() => ocrStore.currentTask);

// 智能显示处理指示器
const showProcessingIndicator = computed(() => {
  // 如果正在上传，显示
  if (isUploading.value) {
    return true;
  }

  // 如果正在处理，显示
  if (isProcessing.value) {
    return true;
  }

  // 如果有任务且任务未完成，显示
  if (currentTask.value && currentTask.value.taskId) {
    const status = currentTask.value.status;
    // 只有在明确完成或失败时才不显示
    if (status === 'COMPLETED' || status === 'FAILED') {
      return false;
    }
    // 其他情况（PENDING、PROCESSING、或状态未知）都显示
    return true;
  }

  return false;
});

// 智能进度步骤计算
const progressSteps = computed(() => {
  if (!showProcessingIndicator.value) return [];

  const steps = [
    { icon: '📄', message: '文档上传', status: 'completed', detail: '' },
    { icon: '🔍', message: '文字识别处理', status: 'pending', detail: '' },
    { icon: '🤖', message: 'AI内容分析', status: 'pending', detail: '' },
    { icon: '✨', message: '结果整理', status: 'pending', detail: '' }
  ];

  // 如果正在上传，第一步为active
  if (isUploading.value) {
    steps[0].status = 'active';
    steps[0].detail = '正在上传到服务器...';
    return steps;
  }

  // 如果有任务，根据任务创建时间和状态模拟进度
  if (currentTask.value && currentTask.value.createdAt) {
    try {
      const createdAt = new Date(currentTask.value.createdAt);
      const now = new Date();
      const elapsedSeconds = Math.floor((now.getTime() - createdAt.getTime()) / 1000);

      // 根据选择的模型调整时间阶段
      const isFlashModel = formValue.value.geminiModel === 'gemini-2.5-flash-preview-05-20';
      const timeStages = isFlashModel
        ? { ocr: 8, analysis: 20, finish: 25 }  // Flash模型时间点
        : { ocr: 15, analysis: 40, finish: 50 }; // Pro模型时间点

      if (elapsedSeconds < timeStages.ocr) {
        // OCR阶段
        steps[1].status = 'active';
        steps[1].detail = '正在识别文档内容...';
      } else if (elapsedSeconds < timeStages.analysis) {
        // AI分析阶段
        steps[1].status = 'completed';
        steps[2].status = 'active';
        steps[2].detail = `正在使用${isFlashModel ? 'Gemini 2.5 Flash' : 'Gemini 2.5 Pro'}分析...`;
      } else if (elapsedSeconds < timeStages.finish) {
        // 结果整理阶段
        steps[1].status = 'completed';
        steps[2].status = 'completed';
        steps[3].status = 'active';
        steps[3].detail = '即将完成...';
      } else {
        // 超时但未完成，显示延迟状态
        steps[1].status = 'completed';
        steps[2].status = 'completed';
        steps[3].status = 'active';
        steps[3].detail = '处理时间较长，请耐心等待...';
      }
    } catch (e) {
      console.error('计算进度步骤时出错:', e);
      // 出错时显示默认状态
      steps[1].status = 'active';
      steps[1].detail = '正在处理中...';
    }
  }

  return steps;
});
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



// 批量自定义上传请求 - 现在只收集文件，不立即上传
const batchCustomRequest = ({ file, fileList }) => {
  if (!file) return;

  console.log('收集文件:', file);

  // 处理单个文件
  const actualFile = file instanceof File ? file : file.file;
  if (actualFile instanceof File) {
    // 检查文件是否已存在
    const exists = selectedFiles.value.some(f => f.name === actualFile.name && f.size === actualFile.size);
    if (!exists) {
      selectedFiles.value.push(actualFile);
      console.log(`文件已添加: ${actualFile.name}`);
    } else {
      console.log(`文件已存在，跳过: ${actualFile.name}`);
    }
  } else {
    console.error('无效的文件对象:', file);
  }

  // 阻止默认上传行为
  return false;
};

// 保留原有的单文件上传函数（备用）
const customRequest = ({ file }) => {
  if (!file) return;

  console.log('单文件上传:', file);

  const actualFile = file instanceof File ? file : file.file;
  if (actualFile instanceof File) {
    uploadedFile.value = actualFile;
    ocrStore.uploadFile(actualFile, {
      usePypdf2: formValue.value.usePypdf2,
      useDocling: formValue.value.useDocling,
      useGemini: formValue.value.useGemini,
      useVisionOcr: formValue.value.useVisionOcr,
      forceOcr: formValue.value.forceOcr,
      language: formValue.value.language,
      geminiModel: formValue.value.geminiModel
    });
  } else {
    console.error('无效的文件对象:', file);
  }
};

// 文件管理相关函数
const addMoreFiles = () => {
  if (fileInputRef.value) {
    fileInputRef.value.click();
  }
};

const handleFileInputChange = (event) => {
  const files = Array.from(event.target.files || []);
  files.forEach(file => {
    const exists = selectedFiles.value.some(f => f.name === file.name && f.size === file.size);
    if (!exists) {
      selectedFiles.value.push(file);
    }
  });
  // 清空input以允许重复选择相同文件
  event.target.value = '';
};

const removeFile = (index) => {
  selectedFiles.value.splice(index, 1);
};

const clearAllFiles = () => {
  selectedFiles.value = [];
  if (uploadRef.value) {
    uploadRef.value.clear();
  }
};

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const getFileType = (fileName) => {
  const ext = fileName.split('.').pop()?.toLowerCase();
  const typeMap = {
    'pdf': 'PDF文档',
    'jpg': '图片', 'jpeg': '图片', 'png': '图片', 'tiff': '图片', 'tif': '图片', 'bmp': '图片',
    'xlsx': 'Excel', 'xls': 'Excel', 'xlsm': 'Excel',
    'docx': 'Word', 'doc': 'Word',
    'txt': '文本', 'md': 'Markdown', 'rtf': 'RTF',
    'csv': 'CSV', 'tsv': 'TSV'
  };
  return typeMap[ext] || '未知格式';
};

const startBatchProcessing = async () => {
  if (selectedFiles.value.length === 0) return;

  console.log('开始批量处理', selectedFiles.value.length, '个文件');

  // 立即切换到批量模式
  isBatchMode.value = true;
  batchTasks.value = [];
  batchId.value = `batch_${Date.now()}`;

  // 立即为每个文件创建任务条目
  for (const file of selectedFiles.value) {
    batchTasks.value.push({
      id: `${batchId.value}_${file.name}`,
      fileName: file.name,
      fileSize: file.size,
      status: 'PENDING',
      progress: 0,
      result: null,
      error: null
    });
  }

  // 异步处理每个文件（不等待）
  selectedFiles.value.forEach(async (file, index) => {
    try {
      console.log(`开始处理文件: ${file.name}`);

      // 更新状态为处理中
      batchTasks.value[index].status = 'PROCESSING';

      const response = await ocrStore.uploadFile(file, {
        usePypdf2: formValue.value.usePypdf2,
        useDocling: formValue.value.useDocling,
        useGemini: formValue.value.useGemini,
        useVisionOcr: formValue.value.useVisionOcr,
        forceOcr: formValue.value.forceOcr,
        language: formValue.value.language,
        geminiModel: formValue.value.geminiModel
      });

      if (response && response.taskId) {
        // 更新现有任务条目
        batchTasks.value[index].taskId = response.taskId;
        batchTasks.value[index].status = response.status;
        batchTasks.value[index].createdAt = response.createdAt;
        console.log(`✅ 文件 ${file.name} 上传成功，任务ID: ${response.taskId}, 状态: ${response.status}`);
        
        // 立即检查一次状态，防止状态更新延迟
        setTimeout(async () => {
          try {
            console.log(`🔍 立即检查任务状态: ${file.name} (${response.taskId})`);
            const status = await ocrStore.getTaskStatus(response.taskId);
            if (status && status.status !== response.status) {
              console.log(`📊 状态已更新: ${file.name} ${response.status} → ${status.status}`);
              batchTasks.value[index].status = status.status;
            }
          } catch (e) {
            console.error('立即检查状态失败:', e);
          }
        }, 1000); // 1秒后检查一次
      } else {
        console.error(`❌ 文件 ${file.name} 上传响应无效:`, response);
      }
    } catch (error) {
      console.error(`文件 ${file.name} 上传失败:`, error);
      // 更新现有任务条目为失败状态
      batchTasks.value[index].status = 'FAILED';
      batchTasks.value[index].error = error.message;
    }
  });

  // 清空已选择的文件列表
  selectedFiles.value = [];
  if (uploadRef.value) {
    uploadRef.value.clear();
  }

  // 开始轮询批量任务状态
  startBatchPolling();
};

// 重置表单
const resetForm = () => {
  // 停止批量轮询
  stopBatchPolling();

  ocrStore.reset();
  uploadedFile.value = null; // 清除上传的文件

  // 重置批量处理状态
  batchTasks.value = [];
  batchId.value = null;
  isBatchMode.value = false;
  selectedFiles.value = [];
  selectedTaskId.value = null;
  batchTaskResults.value.clear();

  if (uploadRef.value) {
    uploadRef.value.clear();
  }
  formValue.value = {
    usePypdf2: true,
    useDocling: false,  // 隐藏Docling选项，设为false
    useGemini: true,
    useVisionOcr: false,
    forceOcr: false,
    language: 'auto',
    geminiModel: 'gemini-2.5-flash-preview-05-20'
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

// 获取处理消息
const getProcessingMessage = () => {
  if (!currentTask.value) return '正在初始化...';

  const isFlashModel = formValue.value.geminiModel === 'gemini-2.5-flash-preview-05-20';
  const estimatedTime = isFlashModel ? '约25秒' : '约50秒';

  // 如果正在上传
  if (isUploading.value) {
    return '正在上传文档到服务器...';
  }

  // 根据任务状态显示不同消息
  if (currentTask.value.status === 'PENDING') {
    return `文档已上传，等待处理中... 预计需要${estimatedTime}`;
  } else if (currentTask.value.status === 'PROCESSING') {
    return `正在进行OCR识别和AI分析... 预计需要${estimatedTime}`;
  } else if (currentTask.value.taskId && !currentTask.value.status) {
    return `文档上传成功，正在初始化处理... 预计需要${estimatedTime}`;
  }

  return `正在处理文档，请稍候... 预计需要${estimatedTime}`;
};

// 批量任务状态监听器
let batchPollingInterval = null;

const startBatchPolling = () => {
  if (batchPollingInterval) {
    clearInterval(batchPollingInterval);
  }

  batchPollingInterval = setInterval(async () => {
    if (!isBatchMode.value || batchTasks.value.length === 0) {
      return;
    }

    console.log('🔄 批量轮询检查 - 当前任务数:', batchTasks.value.length);

    // 检查未完成的任务
    const pendingTasks = batchTasks.value.filter(task =>
      task.taskId && (task.status === 'PENDING' || task.status === 'PROCESSING')
    );

    console.log('📋 未完成任务数:', pendingTasks.length, '任务列表:', pendingTasks.map(t => ({
      fileName: t.fileName,
      taskId: t.taskId,
      status: t.status
    })));

    if (pendingTasks.length === 0) {
      // 所有任务都已完成，停止轮询
      console.log('✅ 所有批量任务已完成，停止轮询');
      stopBatchPolling();
      return;
    }

    // 更新任务状态
    for (const task of pendingTasks) {
      try {
        console.log(`🔍 检查任务状态: ${task.fileName} (${task.taskId})`);
        const status = await ocrStore.getTaskStatus(task.taskId);
        console.log(`📊 获取到状态:`, status);
        
        if (status) {
          const taskIndex = batchTasks.value.findIndex(t => t.taskId === task.taskId);
          if (taskIndex !== -1) {
            const oldStatus = batchTasks.value[taskIndex].status;
            
            batchTasks.value[taskIndex] = {
              ...batchTasks.value[taskIndex],
              status: status.status
            };

            console.log(`🔄 状态更新: ${task.fileName} ${oldStatus} → ${status.status}`);

            // 如果任务完成，自动获取结果
            if (status.status === 'COMPLETED' && !batchTaskResults.value.has(task.taskId)) {
              try {
                console.log(`🎯 任务完成，获取结果: ${task.fileName}`);
                const result = await ocrStore.getTaskResult(task.taskId);
                if (result) {
                  batchTaskResults.value.set(task.taskId, result);
                  batchTasks.value[taskIndex].completedAt = result.completedAt;

                  // 如果这是第一个完成的任务，自动选中它
                  if (!selectedTaskId.value) {
                    selectedTaskId.value = task.taskId;
                    console.log(`🎨 自动选中任务: ${task.fileName}`);
                  }
                }
              } catch (error) {
                console.error('自动获取任务结果失败:', error);
              }
            }
          }
        }
      } catch (error) {
        console.error('更新任务状态失败:', error);
      }
    }
  }, 1500); // 改为1.5秒检查一次，更频繁
};

const stopBatchPolling = () => {
  if (batchPollingInterval) {
    clearInterval(batchPollingInterval);
    batchPollingInterval = null;
  }
};

// 监听批量模式变化
watch(isBatchMode, (newValue) => {
  if (newValue && batchTasks.value.length > 0) {
    startBatchPolling();
  } else {
    stopBatchPolling();
  }
});

// 组件卸载时清理
onUnmounted(() => {
  stopBatchPolling();
});

// 批量处理相关辅助函数
const getBatchStatusCount = (status) => {
  return batchTasks.value.filter(task => task.status === status).length;
};

const getTaskStatusText = (status) => {
  switch (status) {
    case 'PENDING':
      return '等待处理';
    case 'PROCESSING':
      return '处理中';
    case 'COMPLETED':
      return '已完成';
    case 'FAILED':
      return '处理失败';
    default:
      return '未知状态';
  }
};

// 批量任务结果管理函数
const selectTask = (task) => {
  if (task.status === 'COMPLETED' && task.taskId) {
    selectedTaskId.value = task.taskId;
    viewTaskResult(task);
  }
};

const viewTaskResult = async (task) => {
  if (!task.taskId) return;

  try {
    // 如果结果已缓存，直接使用
    if (batchTaskResults.value.has(task.taskId)) {
      selectedTaskId.value = task.taskId;
      return;
    }

    // 获取任务结果
    const result = await ocrStore.getTaskResult(task.taskId);
    if (result) {
      batchTaskResults.value.set(task.taskId, result);
      selectedTaskId.value = task.taskId;

      // 更新批量任务状态
      const taskIndex = batchTasks.value.findIndex(t => t.taskId === task.taskId);
      if (taskIndex !== -1) {
        batchTasks.value[taskIndex] = {
          ...batchTasks.value[taskIndex],
          status: result.status,
          completedAt: result.completedAt
        };
      }
    }
  } catch (error) {
    console.error('获取任务结果失败:', error);
  }
};

const getSelectedTask = () => {
  return batchTasks.value.find(task => task.taskId === selectedTaskId.value);
};

const getSelectedTaskResult = () => {
  return selectedTaskId.value ? batchTaskResults.value.get(selectedTaskId.value) : null;
};

const getCompletedTaskOptions = () => {
  return batchTasks.value
    .filter(task => task.status === 'COMPLETED')
    .map(task => ({
      label: task.fileName,
      value: task.taskId
    }));
};

const refreshBatchTaskResult = async () => {
  if (!selectedTaskId.value) return;

  try {
    const result = await ocrStore.getTaskResult(selectedTaskId.value);
    if (result) {
      batchTaskResults.value.set(selectedTaskId.value, result);
    }
  } catch (error) {
    console.error('刷新任务结果失败:', error);
  }
};

const calculateProcessingTime = (task, result) => {
  if (!task?.createdAt || !result?.completedAt) {
    return '未知';
  }

  try {
    const createdAt = new Date(task.createdAt);
    const completedAt = new Date(result.completedAt);
    const diffMs = completedAt.getTime() - createdAt.getTime();

    if (diffMs < 1000) {
      return `${diffMs}毫秒`;
    }

    const diffSec = Math.floor(diffMs / 1000);
    return `${diffSec}秒`;
  } catch (e) {
    console.error('计算处理耗时出错:', e);
    return '计算错误';
  }
};

const formatAnalysis = (analysis) => {
  if (!analysis) return '';

  // 检查analysis是否为错误对象
  if (analysis.error) {
    return `<span style="color: red;">分析失败: ${analysis.error}</span>`;
  }

  // 如果是字符串，进行格式化
  if (typeof analysis === 'string') {
    return analysis
      .replace(/\n/g, '<br>')
      .replace(/#{1,6}\s+(.*?)(?:\n|$)/g, '<strong>$1</strong><br>');
  }

  // 如果是对象但不是错误对象，转为JSON字符串
  return JSON.stringify(analysis, null, 2)
    .replace(/\n/g, '<br>')
    .replace(/ /g, '&nbsp;');
};

// 图像处理函数
const previewImage = (image) => {
  // 创建一个新窗口来预览图像
  const imageUrl = `data:${image.mime_type};base64,${image.data}`;
  const newWindow = window.open('', '_blank');
  if (newWindow) {
    newWindow.document.write(`
      <html>
        <head>
          <title>${image.description || image.image_id}</title>
          <style>
            body {
              margin: 0;
              padding: 20px;
              background: #f5f5f5;
              display: flex;
              flex-direction: column;
              align-items: center;
              font-family: Arial, sans-serif;
            }
            .image-info {
              background: white;
              padding: 15px;
              border-radius: 8px;
              margin-bottom: 20px;
              box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            }
            img {
              max-width: 90vw;
              max-height: 80vh;
              border-radius: 8px;
              box-shadow: 0 4px 12px rgba(0,0,0,0.15);
              background: white;
              padding: 10px;
            }
          </style>
        </head>
        <body>
          <div class="image-info">
            <h2>${image.description || image.image_id}</h2>
            <p>页码: 第${image.page_number}页</p>
            <p>格式: ${image.mime_type}</p>
          </div>
          <img src="${imageUrl}" alt="${image.description || image.image_id}" />
        </body>
      </html>
    `);
    newWindow.document.close();
  }
};

const downloadImage = (image) => {
  try {
    // 创建下载链接
    const imageUrl = `data:${image.mime_type};base64,${image.data}`;
    const link = document.createElement('a');
    link.href = imageUrl;

    // 生成文件名
    const extension = image.mime_type === 'image/png' ? 'png' : 'jpg';
    const fileName = `${image.image_id || 'extracted_image'}.${extension}`;
    link.download = fileName;

    // 触发下载
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    message.success(`图像 ${fileName} 下载成功`);
  } catch (error) {
    console.error('下载图像失败:', error);
    message.error('下载图像失败');
  }
};

// 获取详细状态
const getDetailedStatus = () => {
  if (!currentTask.value) return '初始化中';

  if (isUploading.value) return '上传中';

  switch (currentTask.value.status) {
    case 'PENDING':
      return '等待处理';
    case 'PROCESSING':
      return '正在处理';
    case 'COMPLETED':
      return '处理完成';
    case 'FAILED':
      return '处理失败';
    default:
      return currentTask.value.taskId ? '已创建，等待开始' : '初始化中';
  }
};

// 获取进度摘要
const getProgressSummary = () => {
  if (!currentTask.value) return '';

  const completedSteps = progressSteps.value.filter(step => step.status === 'completed').length;
  const totalSteps = progressSteps.value.length;
  const percentage = Math.round((completedSteps / totalSteps) * 100);

  if (isUploading.value) {
    return '正在上传文件...';
  }

  const activeStep = progressSteps.value.find(step => step.status === 'active');
  if (activeStep && activeStep.detail) {
    return activeStep.detail;
  }

  return `${completedSteps}/${totalSteps} 步骤完成 (${percentage}%)`;
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
  padding: 40px 20px;
}

.processing-text {
  text-align: center;
  margin-top: 16px;
}

.processing-text h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.processing-text p {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.processing-info {
  margin-top: 24px;
  text-align: center;
  background-color: rgba(0, 0, 0, 0.02);
  padding: 16px 24px;
  border-radius: 8px;
  font-size: 14px;
  max-width: 400px;
}

.processing-info p {
  margin: 6px 0;
}

/* 现代化进度容器 */
.progress-container {
  margin-top: 24px;
  max-width: 500px;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.progress-header h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.progress-summary {
  font-size: 13px;
  color: #7f8c8d;
  font-weight: 500;
}

.progress-steps {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-step {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
  background-color: rgba(255, 255, 255, 0.7);
  border: 1px solid transparent;
  min-height: 60px;
}

.progress-step.active {
  background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
  border-color: #2196f3;
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.15);
}

.progress-step.completed {
  background: linear-gradient(135deg, #e8f5e8 0%, #f1f8e9 100%);
  border-color: #4caf50;
}

.step-left {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  padding-top: 4px;
}

.step-icon {
  font-size: 18px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.8);
  flex-shrink: 0;
}

.step-info {
  flex: 1;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 2px;
}

.step-subtitle {
  font-size: 12px;
  color: #7f8c8d;
  line-height: 1.4;
}

.step-right {
  display: flex;
  align-items: flex-start;
  padding-top: 4px;
}

.step-status {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-completed {
  color: #4caf50;
  font-size: 16px;
  font-weight: bold;
}

.status-active {
  color: #2196f3;
}

.status-pending {
  color: #bdc3c7;
  font-size: 14px;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
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



.model-description {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  line-height: 1.4;
}

/* 批量处理样式 */
.batch-processing-section {
  margin: 20px 0;
  padding: 20px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  border: 1px solid #dee2e6;
}

.batch-header {
  text-align: center;
  margin-bottom: 20px;
}

.batch-header h3 {
  margin: 0 0 8px 0;
  color: #2c3e50;
  font-size: 18px;
}

.batch-header p {
  margin: 0;
  color: #6c757d;
  font-size: 14px;
}

.batch-progress {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.batch-summary {
  display: flex;
  justify-content: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 8px;
}

.batch-tasks {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.batch-task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  transition: all 0.2s ease;
  cursor: pointer;
}

.batch-task-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.batch-task-item.selected {
  border-color: #007bff;
  background: linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%);
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.2);
}

.batch-task-item.completed {
  border-color: #28a745;
  background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
}

.batch-task-item.failed {
  border-color: #dc3545;
  background: linear-gradient(135deg, #f8d7da 0%, #f5c6cb 100%);
}

.batch-task-item.processing {
  border-color: #007bff;
  background: linear-gradient(135deg, #d1ecf1 0%, #bee5eb 100%);
}

.task-info {
  flex: 1;
  min-width: 0;
}

.task-name {
  font-weight: 500;
  color: #2c3e50;
  font-size: 14px;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-status {
  font-size: 12px;
  color: #6c757d;
}

.task-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.task-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.status-icon {
  font-size: 16px;
  font-weight: bold;
}

.status-icon.success {
  color: #28a745;
}

.status-icon.error {
  color: #dc3545;
}

.status-icon.pending {
  color: #6c757d;
}

/* 文件管理样式 */
.file-management {
  margin: 16px 0;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #e9ecef;
}

.file-management-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.file-management-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: white;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  transition: all 0.2s ease;
}

.file-item:hover {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-color: #007bff;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.file-icon {
  color: #007bff;
  font-size: 20px;
  flex-shrink: 0;
}

.file-details {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-weight: 500;
  color: #2c3e50;
  font-size: 14px;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  font-size: 12px;
  color: #6c757d;
}

/* 图像显示样式 */
.result-images {
  padding: 16px;
}

.images-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.image-item {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.image-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.image-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.image-header h4 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  flex: 1;
  margin-right: 8px;
}

.image-content {
  margin-bottom: 12px;
  text-align: center;
}

.extracted-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.extracted-image:hover {
  border-color: #1890ff;
  transform: scale(1.02);
}

.image-actions {
  display: flex;
  justify-content: center;
}

/* 富文本显示样式 */
.rich-text-content {
  background-color: #fafafa;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e0e0e0;
}

/* 全局加载指示器已移除 */
</style>
