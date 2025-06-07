<template>
  <n-card title="处理历史" class="history-card">
    <template #header-extra>
      <n-space>
        <n-button size="small" @click="handleRefresh" :loading="isLoadingTaskList">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新
        </n-button>
        <n-button size="small" @click="clearCompleted" :disabled="completedTasksCount === 0">
          <template #icon>
            <n-icon><TrashOutline /></n-icon>
          </template>
          清理已完成
        </n-button>
      </n-space>
    </template>

    <!-- 统计信息 -->
    <div class="stats-section">
      <n-space>
        <n-statistic label="总任务数" :value="taskListTotal" />
        <n-statistic label="已完成" :value="completedTasksCount" />
        <n-statistic label="失败" :value="failedTasksCount" />
        <n-statistic label="进行中" :value="activeTasksCount" />
      </n-space>
    </div>

    <!-- 任务列表 -->
    <div class="task-list">
      <n-empty v-if="(!taskList || taskList.length === 0) && !isLoadingTaskList" description="暂无处理记录">
        <template #extra>
          <n-button size="small" @click="$emit('refresh')">刷新列表</n-button>
        </template>
      </n-empty>

      <n-list v-else>
        <n-list-item 
          v-for="task in taskList" 
          :key="task.taskId"
          class="task-item"
        >
          <div class="task-content">
            <!-- 任务基本信息 -->
            <div class="task-header">
              <n-space justify="space-between" align="center">
                <div class="task-info">
                  <n-space align="center">
                    <!-- 状态标签 -->
                    <n-tag :type="getStatusTagType(task.status)" size="small">
                      {{ getStatusText(task.status) }}
                    </n-tag>
                    
                    <!-- 类型标签 -->
                    <n-tag :type="task.linkType === 'VIDEO' ? 'info' : 'success'" size="small">
                      {{ task.linkType === 'VIDEO' ? '视频' : '网页' }}
                    </n-tag>
                    
                    <!-- 时间信息 -->
                    <n-text depth="3" size="small">
                      {{ formatTime(task.createdAt) }}
                    </n-text>
                  </n-space>
                </div>
                
                <!-- 操作按钮 -->
                <n-space>
                  <n-button 
                    size="tiny" 
                    text 
                    @click="handleTaskSelect(task)"
                    :disabled="task.status !== 'COMPLETED'"
                  >
                    查看结果
                  </n-button>
                  <n-button 
                    size="tiny" 
                    text 
                    type="error"
                    @click="handleTaskDelete(task)"
                  >
                    删除
                  </n-button>
                </n-space>
              </n-space>
            </div>

            <!-- 任务详情 -->
            <div class="task-details">
              <!-- URL显示 -->
              <div class="task-url">
                <n-ellipsis style="max-width: 500px;">
                  <n-button text size="tiny" @click="openUrl(task.url)">
                    {{ task.url }}
                  </n-button>
                </n-ellipsis>
              </div>

              <!-- 视频标题（如果有） -->
              <div v-if="task.videoTitle" class="task-title">
                <n-text strong>{{ task.videoTitle }}</n-text>
              </div>

              <!-- 处理结果预览 -->
              <div v-if="task.summary" class="task-summary">
                <n-ellipsis :line-clamp="2" :tooltip="false">
                  {{ task.summary }}
                </n-ellipsis>
              </div>

              <!-- 错误信息 -->
              <div v-if="task.status === 'FAILED' && task.errorMessage" class="task-error">
                <n-text type="error" depth="2">
                  错误: {{ task.errorMessage }}
                </n-text>
              </div>

              <!-- 进度指示器（处理中的任务） -->
              <div v-if="task.status === 'PROCESSING'" class="task-progress">
                <n-progress 
                  type="line" 
                  :percentage="50" 
                  status="active"
                  :show-indicator="false"
                  :height="4"
                />
                <n-text depth="3" size="small">正在处理中...</n-text>
              </div>
            </div>
          </div>
        </n-list-item>
      </n-list>

      <!-- 分页 -->
      <div v-if="taskListTotal > taskListSize" class="pagination">
        <n-pagination
          v-model:page="currentPage"
          :page-count="Math.ceil(taskListTotal / taskListSize)"
          :page-size="taskListSize"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </div>

    <!-- 加载状态 -->
    <n-spin v-if="isLoadingTaskList" class="loading-overlay" />
  </n-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { RefreshOutline, TrashOutline } from '@vicons/ionicons5';
import { useDialog, useMessage } from 'naive-ui';
import { useLinkProcessingStore } from '../stores/linkProcessingStore';
import type { TaskDetailResponse, TaskStatus } from '../types/linkProcessing';

// Emits
defineEmits<{
  taskSelected: [taskId: string];
  refresh: [];
}>();

// Store和服务
const linkProcessingStore = useLinkProcessingStore();
const dialog = useDialog();
const message = useMessage();

// 响应式数据
const currentPage = ref(1);

// 计算属性
const taskList = computed(() => linkProcessingStore.taskList);
const taskListTotal = computed(() => linkProcessingStore.taskListTotal);
const taskListSize = computed(() => linkProcessingStore.taskListSize);
const isLoadingTaskList = computed(() => linkProcessingStore.isLoadingTaskList);
const completedTasksCount = computed(() => linkProcessingStore.completedTasksCount);
const failedTasksCount = computed(() => linkProcessingStore.failedTasksCount);

const activeTasksCount = computed(() => {
  return (taskList.value || []).filter(task => 
    task.status === 'PENDING' || task.status === 'PROCESSING'
  ).length;
});

// 方法
const getStatusTagType = (status: TaskStatus) => {
  switch (status) {
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

const getStatusText = (status: TaskStatus) => {
  switch (status) {
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

const formatTime = (timeStr: string) => {
  try {
    const date = new Date(timeStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffMinutes < 1) {
      return '刚刚';
    } else if (diffMinutes < 60) {
      return `${diffMinutes}分钟前`;
    } else if (diffHours < 24) {
      return `${diffHours}小时前`;
    } else if (diffDays < 7) {
      return `${diffDays}天前`;
    } else {
      return date.toLocaleDateString('zh-CN');
    }
  } catch {
    return timeStr;
  }
};

const handleRefresh = () => {
  linkProcessingStore.loadTaskList(currentPage.value - 1, taskListSize.value);
};

const handleTaskSelect = (task: TaskDetailResponse) => {
  if (task.status === 'COMPLETED') {
    // 触发父组件的任务选择事件
    linkProcessingStore.getTaskDetail(task.taskId);
  }
};

const handleTaskDelete = (task: TaskDetailResponse) => {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除这个${task.linkType === 'VIDEO' ? '视频转写' : '网页摘要'}任务吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await linkProcessingStore.deleteTask(task.taskId);
      } catch (error) {
        // 错误已在store中处理
      }
    }
  });
};

const clearCompleted = () => {
  const completedTasks = (taskList.value || []).filter(task => task.status === 'COMPLETED');
  
  if (completedTasks.length === 0) {
    message.warning('没有已完成的任务需要清理');
    return;
  }

  dialog.warning({
    title: '确认清理',
    content: `确定要删除所有${completedTasks.length}个已完成的任务吗？此操作不可撤销。`,
    positiveText: '清理',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 批量删除已完成的任务
        const deletePromises = completedTasks.map(task => 
          linkProcessingStore.deleteTask(task.taskId)
        );
        
        await Promise.all(deletePromises);
        message.success(`已清理${completedTasks.length}个已完成的任务`);
      } catch (error) {
        message.error('清理任务时发生错误');
      }
    }
  });
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
  linkProcessingStore.loadTaskList(page - 1, taskListSize.value);
};

const handlePageSizeChange = (pageSize: number) => {
  currentPage.value = 1;
  linkProcessingStore.loadTaskList(0, pageSize);
};

const openUrl = (url: string) => {
  window.open(url, '_blank');
};

// 生命周期
onMounted(() => {
  // 初始加载任务列表
  if (taskList.value.length === 0) {
    handleRefresh();
  }
});
</script>

<style scoped>
.history-card {
  position: relative;
}

.stats-section {
  margin-bottom: 24px;
  padding: 16px;
  background-color: #fafafa;
  border-radius: 6px;
}

.task-list {
  margin-top: 16px;
}

.task-item {
  border-bottom: 1px solid #f0f0f0;
  padding: 16px 0;
}

.task-item:last-child {
  border-bottom: none;
}

.task-content {
  width: 100%;
}

.task-header {
  margin-bottom: 12px;
}

.task-details {
  margin-left: 8px;
}

.task-url {
  margin-bottom: 8px;
}

.task-title {
  margin-bottom: 8px;
}

.task-summary {
  margin-bottom: 8px;
  color: #666;
  line-height: 1.5;
}

.task-error {
  margin-bottom: 8px;
}

.task-progress {
  margin-top: 8px;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.loading-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .task-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .task-info {
    margin-bottom: 8px;
  }
  
  .stats-section :deep(.n-statistic) {
    margin-bottom: 8px;
  }
}

/* 动画效果 */
.task-item {
  transition: background-color 0.2s ease;
}

.task-item:hover {
  background-color: #fafafa;
}

/* 进度条动画 */
:deep(.n-progress .n-progress-graph .n-progress-graph-line-fill) {
  animation: progress-flow 2s linear infinite;
}

@keyframes progress-flow {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 40px 0;
  }
}
</style>
