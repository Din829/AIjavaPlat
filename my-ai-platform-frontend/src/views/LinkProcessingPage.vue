<template>
  <div class="link-processing-page">
    <!-- 页面标题和服务状态 -->
    <div class="page-header">
      <n-space justify="space-between" align="center">
        <div>
          <h1>智能链接处理</h1>
          <p class="page-description">
            支持视频转写（YouTube、Bilibili等）和网页摘要功能
          </p>
        </div>
        
        <!-- 服务状态指示器 -->
        <n-space>
          <n-tag 
            :type="serviceHealth.videoService ? 'success' : 'error'" 
            size="small"
            @click="checkServiceHealth"
            style="cursor: pointer"
          >
            视频服务: {{ serviceHealth.videoService ? '正常' : '异常' }}
          </n-tag>
          <n-tag 
            :type="serviceHealth.whisperService ? 'success' : 'error'" 
            size="small"
            @click="checkServiceHealth"
            style="cursor: pointer"
          >
            转写服务: {{ serviceHealth.whisperService ? '正常' : '异常' }}
          </n-tag>
        </n-space>
      </n-space>
    </div>

    <!-- 主要内容区域 -->
    <n-card class="main-card">
      <!-- URL输入区域 -->
      <div class="input-section">
        <n-space vertical size="large">
          <!-- URL输入框 -->
          <div>
            <n-input
              v-model:value="formData.url"
              placeholder="请输入网页链接或视频链接（支持YouTube、Bilibili、Vimeo等）"
              size="large"
              clearable
              :disabled="isProcessing"
              @blur="handleUrlBlur"
            />
            
            <!-- 链接分析结果 -->
            <div v-if="linkAnalysis" class="link-analysis-result">
              <n-alert 
                :type="linkAnalysis.isSupported ? 'success' : 'warning'"
                :title="getLinkAnalysisTitle()"
                style="margin-top: 12px"
              >
                {{ linkAnalysis.message }}
                <template v-if="linkAnalysis.platform">
                  <br>
                  <n-text depth="3">平台: {{ linkAnalysis.platform }}</n-text>
                </template>
              </n-alert>
            </div>
          </div>

          <!-- 高级选项 -->
          <n-collapse>
            <n-collapse-item title="高级选项" name="advanced">
              <n-space vertical>
                <n-form-item label="语言选择">
                  <n-select 
                    v-model:value="formData.language" 
                    :options="languageOptions"
                    placeholder="选择语言（默认自动检测）"
                    clearable
                    :disabled="isProcessing"
                  />
                </n-form-item>
                
                <n-form-item label="自定义提示词">
                  <n-input
                    v-model:value="formData.customPrompt"
                    type="textarea"
                    placeholder="可选：提供自定义的分析指令，例如'重点关注技术要点'、'总结商业价值'等"
                    :rows="3"
                    :disabled="isProcessing"
                  />
                </n-form-item>
              </n-space>
            </n-collapse-item>
          </n-collapse>

          <!-- 操作按钮 -->
          <n-space>
            <n-button 
              type="primary" 
              size="large" 
              @click="handleSubmit" 
              :loading="isProcessing"
              :disabled="!formData.url || !linkAnalysis?.isSupported"
            >
              <template #icon>
                <n-icon><PlayCircleOutline /></n-icon>
              </template>
              开始处理
            </n-button>
            
            <n-button 
              size="large" 
              @click="handleClear"
              :disabled="isProcessing"
            >
              清空
            </n-button>
          </n-space>
        </n-space>
      </div>
    </n-card>

    <!-- 处理进度显示 -->
    <div v-if="isProcessing || currentTask" class="progress-section">
      <LinkProcessingProgress 
        v-if="currentTask"
        :task="currentTask" 
        :progress="progress"
        :is-processing="isProcessing"
      />
    </div>

    <!-- 结果展示 -->
    <div v-if="currentTask && currentTask.status === 'COMPLETED'" class="result-section">
      <LinkProcessingResult 
        v-if="currentTask.status === 'COMPLETED'"
        :task="currentTask" 
      />
    </div>

    <!-- 任务历史 -->
    <div class="history-section">
      <LinkProcessingHistory 
        @task-selected="handleTaskSelected"
        @refresh="loadTaskList"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { PlayCircleOutline } from '@vicons/ionicons5';
import { useLinkProcessingStore } from '../stores/linkProcessingStore';
import { LANGUAGE_OPTIONS } from '../types/linkProcessing';
import type { LinkProcessRequest } from '../types/linkProcessing';

// 导入子组件
import LinkProcessingProgress from '../components/LinkProcessingProgress.vue';
import LinkProcessingResult from '../components/LinkProcessingResult.vue';
import LinkProcessingHistory from '../components/LinkProcessingHistory.vue';

// Store
const linkProcessingStore = useLinkProcessingStore();

// 组件状态标志
const isMounted = ref(true);

// 本地状态，用于防止直接响应store变化
const localCurrentTask = ref<any>(null);
const localProgress = ref<any>(null);

// 响应式数据
const formData = ref<LinkProcessRequest>({
  url: '',
  language: 'auto',
  customPrompt: ''
});

// 计算属性
const isProcessing = computed(() => linkProcessingStore.isProcessing);
const currentTask = computed(() => localCurrentTask.value || linkProcessingStore.currentTask);
const progress = computed(() => localProgress.value || linkProcessingStore.progress);
const linkAnalysis = computed(() => linkProcessingStore.linkAnalysis);
const isAnalyzing = computed(() => linkProcessingStore.isAnalyzing);
const serviceHealth = computed(() => linkProcessingStore.serviceHealth);

const languageOptions = LANGUAGE_OPTIONS;

// 监听store状态变化，使用防抖处理
watch(
  () => linkProcessingStore.currentTask,
  (newTask) => {
    if (!isMounted.value) return;
    nextTick(() => {
      localCurrentTask.value = newTask;
    });
  },
  { deep: true }
);

watch(
  () => linkProcessingStore.progress,
  (newProgress) => {
    if (!isMounted.value) return;
    nextTick(() => {
      localProgress.value = newProgress;
    });
  },
  { deep: true }
);

// 方法
const handleUrlBlur = async () => {
  if (!isMounted.value) return; // 防护检查
  
  if (formData.value.url && formData.value.url.trim()) {
    try {
      await linkProcessingStore.analyzeLink(formData.value.url.trim());
    } catch (error) {
      // 错误已在store中处理
    }
  } else {
    linkProcessingStore.clearLinkAnalysis();
  }
};

const getLinkAnalysisTitle = () => {
  if (!linkAnalysis.value) return '';
  
  switch (linkAnalysis.value.linkType) {
    case 'VIDEO':
      return '检测到视频链接';
    case 'WEBPAGE':
      return '检测到网页链接';
    default:
      return '链接类型未知';
  }
};

const handleSubmit = async () => {
  if (!isMounted.value) return; // 防护检查
  
  if (!formData.value.url || !linkAnalysis.value?.isSupported) {
    return;
  }

  try {
    await linkProcessingStore.processLink({
      url: formData.value.url.trim(),
      language: formData.value.language || 'auto',
      customPrompt: formData.value.customPrompt || undefined
    });
  } catch (error) {
    // 错误已在store中处理
  }
};

const handleClear = () => {
  if (!isMounted.value) return; // 防护检查
  
  formData.value = {
    url: '',
    language: 'auto',
    customPrompt: ''
  };
  linkProcessingStore.clearLinkAnalysis();
  linkProcessingStore.clearCurrentProcessing();
};

const handleTaskSelected = (taskId: string) => {
  if (!isMounted.value) return; // 防护检查
  // 加载选中的任务详情
  linkProcessingStore.getTaskDetail(taskId);
};

const loadTaskList = () => {
  if (!isMounted.value) return; // 防护检查
  linkProcessingStore.loadTaskList();
};

const checkServiceHealth = () => {
  if (!isMounted.value) return; // 防护检查
  linkProcessingStore.checkServiceHealth();
};

// 生命周期
onMounted(() => {
  // 初始化时检查服务状态和加载任务列表
  checkServiceHealth();
  loadTaskList();
});

onUnmounted(() => {
  // 组件卸载时设置标志，停止所有异步操作
  isMounted.value = false;
  // 清理当前处理状态，防止内存泄漏
  linkProcessingStore.clearCurrentProcessing();
});
</script>

<style scoped>
.link-processing-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  margin: 0 0 8px 0;
  font-size: 28px;
  font-weight: 600;
  color: #333;
}

.page-description {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.main-card {
  margin-bottom: 24px;
}

.input-section {
  padding: 8px 0;
}

.link-analysis-result {
  animation: fadeIn 0.3s ease-in-out;
}

.progress-section {
  margin-bottom: 24px;
}

.result-section {
  margin-bottom: 24px;
}

.history-section {
  margin-top: 32px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .link-processing-page {
    padding: 16px;
  }
  
  .page-header h1 {
    font-size: 24px;
  }
}
</style>
