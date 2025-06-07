/**
 * 链接处理状态管理 Store
 * 使用 Pinia 管理链接处理相关的状态和操作
 * 
 * @author Ding
 * @since 2024-12-28
 */

import { defineStore } from 'pinia';
import { ref, computed, nextTick } from 'vue';
import LinkProcessingService from '../services/linkProcessingService';
import { useMessageStore } from './messageStore';
import type { 
  LinkProcessRequest,
  LinkAnalysisResponse,
  TaskDetailResponse,
  TaskListResponse,
  ProcessingProgress,
  LinkType,
  TaskStatus
} from '../types/linkProcessing';
import { PROCESSING_STEPS } from '../types/linkProcessing';

export const useLinkProcessingStore = defineStore('linkProcessing', () => {
  // === 状态定义 ===
  
  // 获取 messageStore 实例
  const messageStore = useMessageStore();

  // 当前处理状态
  const isProcessing = ref(false);
  const currentTask = ref<TaskDetailResponse | null>(null);
  const progress = ref<ProcessingProgress | null>(null);
  const error = ref<string | null>(null);
  
  // 任务列表
  const taskList = ref<TaskDetailResponse[]>([]);
  const taskListTotal = ref(0);
  const taskListPage = ref(0);
  const taskListSize = ref(10);
  const isLoadingTaskList = ref(false);
  
  // 链接分析结果
  const linkAnalysis = ref<LinkAnalysisResponse | null>(null);
  const isAnalyzing = ref(false);
  
  // 服务健康状态
  const serviceHealth = ref({
    videoService: false,
    whisperService: false,
    message: '未检查'
  });

  // 轮询控制器
  const pollingController = ref<AbortController | null>(null);

  // === 计算属性 ===
  
  const hasActiveTasks = computed(() => {
    return (taskList.value || []).some(task => 
      task.status === 'PENDING' || task.status === 'PROCESSING'
    );
  });
  
  const completedTasksCount = computed(() => {
    return (taskList.value || []).filter(task => task.status === 'COMPLETED').length;
  });
  
  const failedTasksCount = computed(() => {
    return (taskList.value || []).filter(task => task.status === 'FAILED').length;
  });

  // === 操作方法 ===
  
  /**
   * 分析链接类型
   */
  const analyzeLink = async (url: string): Promise<LinkAnalysisResponse> => {
    isAnalyzing.value = true;
    error.value = null;
    
    try {
      const result = await LinkProcessingService.analyzeLink(url);
      linkAnalysis.value = result;
      return result;
    } catch (err: any) {
      const errorMessage = err.message || '链接分析失败';
      error.value = errorMessage;
      messageStore.error(errorMessage);
      throw err;
    } finally {
      isAnalyzing.value = false;
    }
  };

  /**
   * 创建并开始处理任务
   */
  const processLink = async (request: LinkProcessRequest): Promise<string> => {
    // 如果有正在进行的轮询，先中断
    if (pollingController.value) {
      pollingController.value.abort();
    }
    
    isProcessing.value = true;
    error.value = null;
    progress.value = null;
    
    try {
      // 1. 创建任务
      const createResult = await LinkProcessingService.createTask(request);
      const taskId = createResult.taskId;
      
      // 2. 初始化进度
      updateProgress(createResult.linkType, 0, 'analyze', '开始处理...');
      
      // 3. 创建新的轮询控制器
      pollingController.value = new AbortController();
      
      // 4. 开始轮询任务状态
      const finalTask = await LinkProcessingService.pollTaskStatus(
        taskId,
        (task) => {
          // 使用nextTick确保状态更新的安全性
          nextTick(() => {
            if (currentTask.value?.taskId === taskId || !currentTask.value) {
              currentTask.value = task;
              updateProgressFromTask(task);
            }
          });
        },
        60, // 最大轮询60次
        5000, // 每5秒轮询一次
        pollingController.value.signal // 传递中断信号
      );
      
      // 最终状态更新也使用nextTick
      await nextTick(() => {
        currentTask.value = finalTask;
      });
      
      if (finalTask.status === 'COMPLETED') {
        messageStore.success('处理完成！');
        progress.value = {
          currentStep: getStepsForLinkType(createResult.linkType).length,
          totalSteps: getStepsForLinkType(createResult.linkType).length,
          stepName: 'completed',
          stepDescription: '处理完成',
          percentage: 100,
          status: 'success'
        };
      } else if (finalTask.status === 'FAILED') {
        const errorMsg = finalTask.errorMessage || '处理失败';
        error.value = errorMsg;
        messageStore.error(errorMsg);
        progress.value = {
          currentStep: 0,
          totalSteps: getStepsForLinkType(createResult.linkType).length,
          stepName: 'error',
          stepDescription: errorMsg,
          percentage: 0,
          status: 'error'
        };
      }
      
      // 刷新任务列表
      await loadTaskList();
      
      return taskId;
      
    } catch (err: any) {
      // 如果是中断错误，不显示错误消息
      if (err.message === '轮询已被中断') {
        console.log('任务轮询被用户中断');
        return '';
      }
      
      const errorMessage = err.message || '处理失败';
      error.value = errorMessage;
      messageStore.error(errorMessage);
      throw err;
    } finally {
      isProcessing.value = false;
      pollingController.value = null;
    }
  };

  /**
   * 加载任务列表
   */
  const loadTaskList = async (page: number = 0, size: number = 10): Promise<void> => {
    isLoadingTaskList.value = true;
    
    try {
      const result = await LinkProcessingService.getTaskList(page, size);
      taskList.value = result.tasks;
      taskListTotal.value = result.total;
      taskListPage.value = result.page;
      taskListSize.value = result.size;
    } catch (err: any) {
      const errorMessage = err.message || '加载任务列表失败';
      messageStore.error(errorMessage);
      throw err;
    } finally {
      isLoadingTaskList.value = false;
    }
  };

  /**
   * 获取任务详情
   */
  const getTaskDetail = async (taskId: string): Promise<TaskDetailResponse> => {
    try {
      const task = await LinkProcessingService.getTaskDetail(taskId);
      
      // 如果是当前任务，更新状态
      if (currentTask.value?.taskId === taskId) {
        currentTask.value = task;
      }
      
      return task;
    } catch (err: any) {
      const errorMessage = err.message || '获取任务详情失败';
      messageStore.error(errorMessage);
      throw err;
    }
  };

  /**
   * 删除任务
   */
  const deleteTask = async (taskId: string): Promise<void> => {
    try {
      await LinkProcessingService.deleteTask(taskId);
      messageStore.success('任务删除成功');
      
      // 先检查是否是当前任务，如果是则先清空
      const isCurrentTask = currentTask.value?.taskId === taskId;
      
      if (isCurrentTask) {
        // 使用安全的清理方法
        clearCurrentProcessing();
      }
      
      // 使用nextTick确保状态清理完成后再更新列表
      await nextTick(() => {
        // 从列表中移除
        taskList.value = taskList.value.filter(task => task.taskId !== taskId);
        taskListTotal.value = Math.max(0, taskListTotal.value - 1);
      });
      
    } catch (err: any) {
      const errorMessage = err.message || '删除任务失败';
      messageStore.error(errorMessage);
      throw err;
    }
  };

  /**
   * 检查服务健康状态
   */
  const checkServiceHealth = async (): Promise<void> => {
    try {
      const health = await LinkProcessingService.checkServiceHealth();
      serviceHealth.value = health;
    } catch (err: any) {
      serviceHealth.value = {
        videoService: false,
        whisperService: false,
        message: '服务状态检查失败'
      };
    }
  };

  /**
   * 清空当前处理状态
   */
  const clearCurrentProcessing = (): void => {
    // 中断正在进行的轮询
    if (pollingController.value) {
      pollingController.value.abort();
      pollingController.value = null;
    }
    
    // 按照安全顺序清空状态
    isProcessing.value = false;
    error.value = null;
    progress.value = null;
    
    // 延迟清空currentTask，确保依赖组件有时间处理状态变化
    nextTick(() => {
      currentTask.value = null;
    });
  };

  /**
   * 清空链接分析结果
   */
  const clearLinkAnalysis = (): void => {
    linkAnalysis.value = null;
    error.value = null;
  };

  // === 辅助方法 ===
  
  /**
   * 根据链接类型获取处理步骤
   */
  const getStepsForLinkType = (linkType: LinkType) => {
    return linkType === 'VIDEO' ? PROCESSING_STEPS.VIDEO : PROCESSING_STEPS.WEBPAGE;
  };

  /**
   * 更新处理进度
   */
  const updateProgress = (
    linkType: LinkType, 
    currentStep: number, 
    stepName: string, 
    description: string
  ): void => {
    const steps = getStepsForLinkType(linkType);
    const percentage = Math.round((currentStep / steps.length) * 100);
    
    progress.value = {
      currentStep,
      totalSteps: steps.length,
      stepName,
      stepDescription: description,
      percentage,
      status: 'active'
    };
  };

  /**
   * 根据任务状态更新进度
   */
  const updateProgressFromTask = (task: TaskDetailResponse): void => {
    if (!task.linkType) return;
    
    const steps = getStepsForLinkType(task.linkType);
    let currentStep = 0;
    let stepName = 'analyze';
    let description = '分析中...';
    
    switch (task.status) {
      case 'PENDING':
        currentStep = 0;
        stepName = 'analyze';
        description = '等待处理...';
        break;
      case 'PROCESSING':
        currentStep = 1;
        stepName = task.linkType === 'VIDEO' ? 'download' : 'extract';
        description = task.linkType === 'VIDEO' ? '下载和转换中...' : '提取内容中...';
        break;
      case 'COMPLETED':
        currentStep = steps.length;
        stepName = 'completed';
        description = '处理完成';
        break;
      case 'FAILED':
        currentStep = 0;
        stepName = 'error';
        description = task.errorMessage || '处理失败';
        break;
    }
    
    updateProgress(task.linkType, currentStep, stepName, description);
  };

  return {
    // 状态
    isProcessing,
    currentTask,
    progress,
    error,
    taskList,
    taskListTotal,
    taskListPage,
    taskListSize,
    isLoadingTaskList,
    linkAnalysis,
    isAnalyzing,
    serviceHealth,
    
    // 计算属性
    hasActiveTasks,
    completedTasksCount,
    failedTasksCount,
    
    // 方法
    analyzeLink,
    processLink,
    loadTaskList,
    getTaskDetail,
    deleteTask,
    checkServiceHealth,
    clearCurrentProcessing,
    clearLinkAnalysis
  };
});
