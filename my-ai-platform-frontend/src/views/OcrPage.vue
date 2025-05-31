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

      <!-- 处理中状态 - 智能进度版 -->
      <div v-if="showProcessingIndicator" class="processing-section">
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
                  <pre>{{ resultContent.extractedText }}</pre>
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
  NSpin, NTabs, NTabPane, NEmpty, NScrollbar,
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
const acceptFileTypes = '.pdf,.jpg,.jpeg,.png,.tiff,.tif,.bmp';

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
      useVisionOcr: formValue.value.useVisionOcr,
      forceOcr: formValue.value.forceOcr,
      language: formValue.value.language,
      geminiModel: formValue.value.geminiModel
    });
  } else if (file.file && file.file instanceof File) {
    // 有些UI组件可能会将文件包装在一个对象中
    uploadedFile.value = file.file;
    ocrStore.uploadFile(file.file, {
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

/* 全局加载指示器已移除 */
</style>
