<template>
  <n-card title="处理进度" class="progress-card">
    <template #header-extra>
      <n-tag :type="getStatusTagType()" size="small">
        {{ getStatusText() }}
      </n-tag>
    </template>

    <!-- 任务基本信息 -->
    <div v-if="task" class="task-info">
      <n-descriptions :column="2" size="small">
        <n-descriptions-item label="链接类型">
          <n-tag :type="task.linkType === 'VIDEO' ? 'info' : 'success'" size="small">
            {{ task.linkType === 'VIDEO' ? '视频' : '网页' }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="任务ID">
          <n-text code>{{ task.taskId }}</n-text>
        </n-descriptions-item>
        <n-descriptions-item label="创建时间">
          {{ formatTime(task.createdAt) }}
        </n-descriptions-item>
        <n-descriptions-item label="处理时长" v-if="task.completedAt">
          {{ calculateDuration() }}
        </n-descriptions-item>
      </n-descriptions>
    </div>

    <!-- 进度步骤 -->
    <div class="progress-steps">
      <n-steps 
        :current="currentStepIndex" 
        :status="getStepsStatus()"
        size="small"
      >
        <n-step 
          v-for="(step, index) in steps"
          :key="step.name"
          :title="step.description"
          :description="getStepDescription(index)"
        />
      </n-steps>
    </div>

    <!-- 详细进度条 -->
    <div class="progress-bar">
      <n-progress
        type="line"
        :percentage="progressPercentage"
        :status="getProgressStatus()"
        :show-indicator="true"
        :height="8"
      />
      <div class="progress-text">
        <n-text :depth="2">{{ progressText }}</n-text>
      </div>
    </div>

    <!-- 视频信息（仅视频任务显示） -->
    <div v-if="task && task.linkType === 'VIDEO' && (task.videoTitle || task.videoDescription)" class="video-info">
      <n-divider title-placement="left">视频信息</n-divider>
      <n-descriptions :column="1" size="small">
        <n-descriptions-item v-if="task.videoTitle" label="标题">
          {{ task.videoTitle }}
        </n-descriptions-item>
        <n-descriptions-item v-if="task.videoDuration" label="时长">
          {{ formatDuration(task.videoDuration) }}
        </n-descriptions-item>
        <n-descriptions-item v-if="task.videoDescription" label="描述">
          <n-ellipsis style="max-width: 400px" :tooltip="false">
            {{ task.videoDescription }}
          </n-ellipsis>
        </n-descriptions-item>
      </n-descriptions>
    </div>

    <!-- 错误信息 -->
    <div v-if="task && task.status === 'FAILED'" class="error-info">
      <n-alert type="error" title="处理失败">
        {{ task.errorMessage || '未知错误' }}
      </n-alert>
    </div>

    <!-- 实时状态更新 -->
    <div v-if="isProcessing" class="live-status">
      <n-space align="center">
        <n-spin size="small" />
        <n-text :depth="2">正在处理中，请稍候...</n-text>
        <n-button text size="tiny" @click="$emit('refresh')">
          刷新状态
        </n-button>
      </n-space>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { TaskDetailResponse, ProcessingProgress } from '../types/linkProcessing';
import { PROCESSING_STEPS } from '../types/linkProcessing';

// Props
interface Props {
  task?: TaskDetailResponse | null;
  progress?: ProcessingProgress | null;
  isProcessing?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  task: null,
  progress: null,
  isProcessing: false
});

// Emits
defineEmits<{
  refresh: [];
}>();

// 计算属性
const steps = computed(() => {
  if (!props.task) return PROCESSING_STEPS.WEBPAGE;
  return props.task.linkType === 'VIDEO' ? PROCESSING_STEPS.VIDEO : PROCESSING_STEPS.WEBPAGE;
});

const currentStepIndex = computed(() => {
  if (props.progress) {
    return props.progress.currentStep;
  }
  
  if (!props.task) return 0;
  
  switch (props.task.status) {
    case 'PENDING':
      return 0;
    case 'PROCESSING':
      return 1;
    case 'COMPLETED':
      return steps.value.length;
    case 'FAILED':
      return 0;
    default:
      return 0;
  }
});

const progressPercentage = computed(() => {
  if (props.progress) {
    return props.progress.percentage;
  }
  
  if (!props.task) return 0;
  
  switch (props.task.status) {
    case 'PENDING':
      return 10;
    case 'PROCESSING':
      return 50;
    case 'COMPLETED':
      return 100;
    case 'FAILED':
      return 0;
    default:
      return 0;
  }
});

const progressText = computed(() => {
  if (props.progress) {
    return props.progress.stepDescription;
  }
  
  if (!props.task) return '准备中...';
  
  switch (props.task.status) {
    case 'PENDING':
      return '等待处理...';
    case 'PROCESSING':
      return '正在处理中...';
    case 'COMPLETED':
      return '处理完成';
    case 'FAILED':
      return props.task.errorMessage || '处理失败';
    default:
      return '未知状态';
  }
});

// 方法
const getStatusTagType = () => {
  if (!props.task) return 'default';
  
  switch (props.task.status) {
    case 'PENDING':
      return 'warning';
    case 'PROCESSING':
      return 'info';
    case 'COMPLETED':
      return 'success';
    case 'FAILED':
      return 'error';
    default:
      return 'default';
  }
};

const getStatusText = () => {
  if (!props.task) return '准备中';
  
  switch (props.task.status) {
    case 'PENDING':
      return '等待中';
    case 'PROCESSING':
      return '处理中';
    case 'COMPLETED':
      return '已完成';
    case 'FAILED':
      return '失败';
    default:
      return '未知';
  }
};

const getStepsStatus = () => {
  if (!props.task) return 'process';
  
  switch (props.task.status) {
    case 'COMPLETED':
      return 'finish';
    case 'FAILED':
      return 'error';
    default:
      return 'process';
  }
};

const getProgressStatus = () => {
  if (props.progress) {
    return props.progress.status === 'error' ? 'error' : 'active';
  }
  
  if (!props.task) return 'active';
  
  switch (props.task.status) {
    case 'COMPLETED':
      return 'success';
    case 'FAILED':
      return 'error';
    default:
      return 'active';
  }
};

const getStepDescription = (index: number) => {
  if (index < currentStepIndex.value) {
    return '已完成';
  } else if (index === currentStepIndex.value) {
    return props.isProcessing ? '进行中...' : '当前步骤';
  } else {
    return '等待中';
  }
};

const formatTime = (timeStr: string) => {
  try {
    const date = new Date(timeStr);
    return date.toLocaleString('zh-CN');
  } catch {
    return timeStr;
  }
};

const formatDuration = (seconds: number) => {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;
  
  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  } else {
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  }
};

const calculateDuration = () => {
  if (!props.task?.createdAt || !props.task?.completedAt) return '';
  
  try {
    const start = new Date(props.task.createdAt);
    const end = new Date(props.task.completedAt);
    const diffMs = end.getTime() - start.getTime();
    const diffSeconds = Math.floor(diffMs / 1000);
    
    if (diffSeconds < 60) {
      return `${diffSeconds}秒`;
    } else if (diffSeconds < 3600) {
      const minutes = Math.floor(diffSeconds / 60);
      const seconds = diffSeconds % 60;
      return `${minutes}分${seconds}秒`;
    } else {
      const hours = Math.floor(diffSeconds / 3600);
      const minutes = Math.floor((diffSeconds % 3600) / 60);
      return `${hours}小时${minutes}分钟`;
    }
  } catch {
    return '';
  }
};
</script>

<style scoped>
.progress-card {
  margin-bottom: 16px;
}

.task-info {
  margin-bottom: 24px;
}

.progress-steps {
  margin-bottom: 24px;
}

.progress-bar {
  margin-bottom: 16px;
}

.progress-text {
  margin-top: 8px;
  text-align: center;
}

.video-info {
  margin-top: 16px;
}

.error-info {
  margin-top: 16px;
}

.live-status {
  margin-top: 16px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 6px;
  text-align: center;
}

/* 步骤动画 */
:deep(.n-steps .n-step.n-step--process .n-step-indicator) {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(24, 160, 88, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(24, 160, 88, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(24, 160, 88, 0);
  }
}
</style>
