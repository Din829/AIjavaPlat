<template>
  <n-card v-if="task && task.status === 'COMPLETED'" title="处理结果" class="result-card">
    <template #header-extra>
      <n-space>
        <n-button size="small" @click="copyResult">
          <template #icon>
            <n-icon><CopyOutline /></n-icon>
          </template>
          复制结果
        </n-button>
        <n-button size="small" @click="downloadResult">
          <template #icon>
            <n-icon><DownloadOutline /></n-icon>
          </template>
          下载结果
        </n-button>
      </n-space>
    </template>

    <!-- 任务基本信息 -->
    <div v-if="task.completedAt" class="task-summary">
      <n-descriptions :column="3" size="small">
        <n-descriptions-item label="链接类型">
          <n-tag :type="task.linkType === 'VIDEO' ? 'info' : 'success'" size="small">
            {{ task.linkType === 'VIDEO' ? '视频转写' : '网页摘要' }}
          </n-tag>
        </n-descriptions-item>
        <n-descriptions-item label="处理时间">
          {{ formatTime(task.completedAt) }}
        </n-descriptions-item>
        <n-descriptions-item label="处理时长">
          {{ calculateDuration() }}
        </n-descriptions-item>
      </n-descriptions>
    </div>

    <!-- 结果标签页 -->
    <n-tabs type="line" animated>
      <!-- AI总结 -->
      <n-tab-pane name="summary" tab="AI总结">
        <div class="summary-content">
          <n-alert v-if="!task.summary" type="warning" title="暂无总结">
            总结内容为空或生成失败
          </n-alert>
          <div v-else-if="task.summary" class="content-text">
            <RichTextDisplay :content="task.summary" />
          </div>
        </div>
      </n-tab-pane>

      <!-- 原始内容（仅视频显示转写文本） -->
      <n-tab-pane 
        v-if="task && task.linkType === 'VIDEO' && task.transcriptionText" 
        name="transcription" 
        tab="转写文本"
      >
        <div class="transcription-content">
          <div class="content-header">
            <n-space justify="space-between" align="center">
              <n-text strong>完整转写文本</n-text>
              <n-space>
                <n-text depth="3">
                  字数: {{ task.transcriptionText.length }}
                </n-text>
                <n-button size="tiny" text @click="copyTranscription">
                  复制转写文本
                </n-button>
              </n-space>
            </n-space>
          </div>
          <div class="content-text">
            <n-scrollbar style="max-height: 400px;">
              <pre class="transcription-text">{{ task.transcriptionText }}</pre>
            </n-scrollbar>
          </div>
        </div>
      </n-tab-pane>

      <!-- 分段信息（仅视频显示） -->
      <n-tab-pane 
        v-if="task && task.linkType === 'VIDEO' && transcriptionSegments.length > 0" 
        name="segments" 
        tab="分段信息"
      >
        <div class="segments-content">
          <div class="content-header">
            <n-space justify="space-between" align="center">
              <n-text strong>转写分段 ({{ transcriptionSegments.length }}段)</n-text>
              <n-button size="tiny" text @click="exportSegments">
                导出分段
              </n-button>
            </n-space>
          </div>
          <div class="segments-list">
            <n-scrollbar style="max-height: 500px;">
              <div 
                v-for="(segment, index) in transcriptionSegments" 
                :key="index"
                class="segment-item"
              >
                <div class="segment-header">
                  <n-space justify="space-between" align="center">
                    <n-text depth="2" size="small">
                      第{{ index + 1 }}段
                    </n-text>
                    <n-text depth="3" size="small">
                      {{ formatTime(segment.start) }} - {{ formatTime(segment.end) }}
                    </n-text>
                  </n-space>
                </div>
                <div class="segment-text">
                  {{ segment.text }}
                </div>
              </div>
            </n-scrollbar>
          </div>
        </div>
      </n-tab-pane>

      <!-- 原始数据 -->
      <n-tab-pane name="raw" tab="原始数据">
        <div class="raw-content">
          <n-space vertical>
            <n-space justify="space-between" align="center">
              <n-text strong>JSON格式数据</n-text>
              <n-button size="tiny" text @click="copyRawData">
                复制JSON
              </n-button>
            </n-space>
            <n-code 
              :code="formattedRawData" 
              language="json"
              style="max-height: 400px; overflow-y: auto;"
            />
          </n-space>
        </div>
      </n-tab-pane>
    </n-tabs>

    <!-- 视频元数据（仅视频任务显示） -->
    <div v-if="task.linkType === 'VIDEO'" class="video-metadata">
      <n-divider title-placement="left">视频信息</n-divider>
      <n-descriptions :column="2" size="small">
        <n-descriptions-item v-if="task.videoTitle" label="标题">
          {{ task.videoTitle }}
        </n-descriptions-item>
        <n-descriptions-item v-if="task.videoDuration" label="时长">
          {{ formatDuration(task.videoDuration) }}
        </n-descriptions-item>
        <n-descriptions-item v-if="task.url" label="原始链接">
          <n-button text size="tiny" @click="openOriginalLink">
            {{ task.url }}
          </n-button>
        </n-descriptions-item>
        <n-descriptions-item v-if="task.videoDescription" label="描述" :span="2">
          <n-ellipsis style="max-width: 600px" :tooltip="false">
            {{ task.videoDescription }}
          </n-ellipsis>
        </n-descriptions-item>
      </n-descriptions>
    </div>
  </n-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { CopyOutline, DownloadOutline } from '@vicons/ionicons5';
import { useMessage } from 'naive-ui';
import type { TaskDetailResponse, TranscriptionSegment } from '../types/linkProcessing';
import RichTextDisplay from './RichTextDisplay.vue';

// Props
interface Props {
  task: TaskDetailResponse;
}

const props = defineProps<Props>();

// 消息服务
const message = useMessage();

// 计算属性
const transcriptionSegments = computed((): TranscriptionSegment[] => {
  if (!props.task.resultJson) return [];
  
  try {
    const result = JSON.parse(props.task.resultJson);
    return result.segments || [];
  } catch {
    return [];
  }
});

const formattedRawData = computed(() => {
  if (!props.task.resultJson) return '{}';
  
  try {
    const parsed = JSON.parse(props.task.resultJson);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return props.task.resultJson;
  }
});

// 方法
const formatTime = (timeStr: string | number) => {
  if (typeof timeStr === 'number') {
    // 转写时间戳（秒）
    const minutes = Math.floor(timeStr / 60);
    const seconds = Math.floor(timeStr % 60);
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }
  
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
  if (!props.task.createdAt || !props.task.completedAt) return '';
  
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

const copyToClipboard = async (text: string, successMessage: string) => {
  try {
    await navigator.clipboard.writeText(text);
    message.success(successMessage);
  } catch {
    // 降级方案
    const textArea = document.createElement('textarea');
    textArea.value = text;
    document.body.appendChild(textArea);
    textArea.select();
    document.execCommand('copy');
    document.body.removeChild(textArea);
    message.success(successMessage);
  }
};

const copyResult = () => {
  const content = props.task.summary || '无总结内容';
  copyToClipboard(content, '总结内容已复制到剪贴板');
};

const copyTranscription = () => {
  const content = props.task.transcriptionText || '无转写内容';
  copyToClipboard(content, '转写文本已复制到剪贴板');
};

const copyRawData = () => {
  copyToClipboard(formattedRawData.value, 'JSON数据已复制到剪贴板');
};

const downloadResult = () => {
  const content = `# ${props.task.linkType === 'VIDEO' ? '视频转写' : '网页摘要'}结果

## 基本信息
- 链接: ${props.task.url}
- 处理时间: ${formatTime(props.task.completedAt!)}
- 任务ID: ${props.task.taskId}

## AI总结
${props.task.summary || '无总结内容'}

${props.task.transcriptionText ? `## 转写文本\n${props.task.transcriptionText}` : ''}

## 原始数据
\`\`\`json
${formattedRawData.value}
\`\`\`
`;

  const blob = new Blob([content], { type: 'text/markdown' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${props.task.linkType === 'VIDEO' ? '视频转写' : '网页摘要'}_${props.task.taskId}.md`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  
  message.success('结果已下载');
};

const exportSegments = () => {
  if (transcriptionSegments.value.length === 0) {
    message.warning('无分段数据可导出');
    return;
  }

  const content = transcriptionSegments.value
    .map((segment, index) => 
      `${index + 1}. [${formatTime(segment.start)} - ${formatTime(segment.end)}] ${segment.text}`
    )
    .join('\n\n');

  const blob = new Blob([content], { type: 'text/plain' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `转写分段_${props.task.taskId}.txt`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  
  message.success('分段数据已导出');
};

const openOriginalLink = () => {
  if (props.task.url) {
    window.open(props.task.url, '_blank');
  }
};
</script>

<style scoped>
.result-card {
  margin-bottom: 16px;
}

.task-summary {
  margin-bottom: 24px;
}

.content-header {
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.content-text {
  line-height: 1.6;
}

.transcription-text {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  margin: 0;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}

.segments-list {
  margin-top: 16px;
}

.segment-item {
  margin-bottom: 16px;
  padding: 12px;
  background-color: #fafafa;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}

.segment-header {
  margin-bottom: 8px;
}

.segment-text {
  line-height: 1.5;
  color: #333;
}

.video-metadata {
  margin-top: 24px;
}

.summary-content,
.transcription-content,
.segments-content,
.raw-content {
  padding: 8px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .task-summary :deep(.n-descriptions) {
    --n-th-padding: 8px 4px;
    --n-td-padding: 8px 4px;
  }
  
  .segment-item {
    padding: 8px;
  }
}
</style>
