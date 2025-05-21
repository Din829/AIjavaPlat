import { defineStore } from 'pinia';
import ocrService, { OcrTaskStatus } from '../services/ocrService';
import type { OcrUploadRequest, OcrTaskResponse } from '../services/ocrService';
import { message } from '../services/messageService';

/**
 * OCR状态接口
 */
interface OcrState {
  currentTask: OcrTaskResponse | null;
  tasks: OcrTaskResponse[];
  isUploading: boolean;
  isProcessing: boolean;
  isLoading: boolean;
  error: string | null;
  pollingInterval: number | null;
  errorCount: number; // 添加错误计数属性
}

/**
 * OCR状态管理
 */
export const useOcrStore = defineStore('ocr', {
  state: (): OcrState => ({
    currentTask: null,
    tasks: [],
    isUploading: false,
    isProcessing: false,
    isLoading: false,
    error: null,
    pollingInterval: null,
    errorCount: 0 // 初始化错误计数
  }),

  // 确保在页面加载时重置状态
  hydrate(storeState) {
    // 强制重置处理状态，确保页面加载时不会显示加载指示器
    storeState.isProcessing = false;
    storeState.isUploading = false;
    storeState.isLoading = false;
    storeState.pollingInterval = null;
  },

  getters: {
    /**
     * 是否有正在处理的任务
     */
    hasActiveTask: (state) => {
      return state.currentTask !== null &&
        (state.currentTask.status === OcrTaskStatus.PENDING ||
         state.currentTask.status === OcrTaskStatus.PROCESSING);
    },

    /**
     * 当前任务是否已完成
     */
    isTaskCompleted: (state) => {
      return state.currentTask !== null &&
        state.currentTask.status === OcrTaskStatus.COMPLETED;
    },

    /**
     * 当前任务是否失败
     */
    isTaskFailed: (state) => {
      return state.currentTask !== null &&
        state.currentTask.status === OcrTaskStatus.FAILED;
    }
  },

  actions: {
    /**
     * 上传文件并处理
     * @param file 文件
     * @param options 选项
     */
    async uploadFile(file: File, options: OcrUploadRequest = {}) {
      this.isUploading = true;
      this.error = null;

      try {
        // 设置默认选项
        const defaultOptions: OcrUploadRequest = {
          usePypdf2: true,
          useDocling: true,
          useGemini: true,
          forceOcr: false,
          language: 'auto'
        };

        // 合并选项
        const mergedOptions = { ...defaultOptions, ...options };

        // 上传文件
        const response = await ocrService.uploadFile(file, mergedOptions);

        // 确保response有效且包含taskId
        if (response && response.taskId) {
          console.log('File uploaded successfully, setting currentTask:', response);
          this.currentTask = response;

          // 开始轮询任务状态
          this.startPolling(response.taskId);
        } else {
          console.error('Invalid response from uploadFile:', response);
          this.isProcessing = false;
        }

        // 立即尝试获取一次任务状态，以防任务已经完成
        setTimeout(async () => {
          try {
            console.log('立即检查任务状态:', response.taskId);
            await this.getTaskStatus(response.taskId);

            // 如果任务已完成，获取结果
            if (this.currentTask?.status === OcrTaskStatus.COMPLETED) {
              console.log('任务已完成，获取结果');
              await this.getTaskResult(response.taskId);
            }
          } catch (e) {
            console.error('初始检查任务状态失败:', e);
          }
        }, 500); // 500毫秒后检查，给后端一点处理时间

        message.success('文件上传成功，正在处理中...');
        return response;
      } catch (error: any) {
        const errorMsg = error.message || '上传文件失败';
        this.error = errorMsg;
        message.error(errorMsg);
        throw error;
      } finally {
        this.isUploading = false;
      }
    },

    /**
     * 获取任务状态
     * @param taskId 任务ID
     */
    async getTaskStatus(taskId: string) {
      this.isLoading = true;

      try {
        const response = await ocrService.getTaskStatus(taskId);
        this.currentTask = response;

        // 如果任务已完成或失败，停止轮询
        if (response.status === OcrTaskStatus.COMPLETED ||
            response.status === OcrTaskStatus.FAILED) {
          this.stopPolling();

          if (response.status === OcrTaskStatus.COMPLETED) {
            message.success('OCR处理完成');
          } else if (response.status === OcrTaskStatus.FAILED) {
            message.error(`OCR处理失败: ${response.message || '未知错误'}`);
          }
        }

        return response;
      } catch (error: any) {
        const errorMsg = error.message || '获取任务状态失败';
        this.error = errorMsg;
        message.error(errorMsg);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    /**
     * 获取任务结果
     * @param taskId 任务ID
     */
    async getTaskResult(taskId: string) {
      this.isLoading = true;

      try {
        const response = await ocrService.getTaskResult(taskId);
        this.currentTask = response;
        return response;
      } catch (error: any) {
        const errorMsg = error.message || '获取任务结果失败';
        this.error = errorMsg;
        message.error(errorMsg);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    /**
     * 获取用户的所有OCR任务
     */
    async getUserTasks() {
      this.isLoading = true;

      try {
        const response = await ocrService.getUserTasks();
        this.tasks = response;
        return response;
      } catch (error: any) {
        const errorMsg = error.message || '获取任务列表失败';
        this.error = errorMsg;
        message.error(errorMsg);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    /**
     * 开始轮询任务状态
     * @param taskId 任务ID
     */
    startPolling(taskId: string) {
      // 先停止之前的轮询
      this.stopPolling();

      // 只有在有任务ID且任务状态为PENDING或PROCESSING时才设置isProcessing为true
      if (taskId && this.currentTask &&
          (this.currentTask.status === OcrTaskStatus.PENDING ||
           this.currentTask.status === OcrTaskStatus.PROCESSING)) {
        console.log('Setting isProcessing to true for task:', taskId);
        this.isProcessing = true;
      } else {
        console.log('Not setting isProcessing to true, no valid task or status');
        this.isProcessing = false;
      }

      this.pollingInterval = window.setInterval(async () => {
        // 如果任务已完成或失败，停止轮询
        if (this.currentTask &&
            (this.currentTask.status === OcrTaskStatus.COMPLETED ||
             this.currentTask.status === OcrTaskStatus.FAILED)) {
          this.stopPolling();
          return;
        }

        try {
          // 获取任务状态
          await this.getTaskStatus(taskId);
        } catch (error) {
          console.error('轮询任务状态时发生错误:', error);

          // 连续错误计数
          this.errorCount = (this.errorCount || 0) + 1;

          // 如果连续错误超过3次，停止轮询
          if (this.errorCount >= 3) {
            console.error('连续错误超过3次，停止轮询');
            this.stopPolling();
            // 确保currentTask不为null，并且包含必需的taskId字段
            if (this.currentTask && this.currentTask.taskId) {
              this.currentTask = {
                ...this.currentTask,
                status: OcrTaskStatus.FAILED,
                message: '获取任务状态失败，请稍后重试'
              };
            } else {
              // 如果currentTask为null或没有taskId，创建一个新的失败任务
              this.currentTask = {
                taskId: taskId, // 使用传入的taskId
                status: OcrTaskStatus.FAILED,
                message: '获取任务状态失败，请稍后重试'
              };
            }
          }
        }
      }, 1000); // 每1秒轮询一次
    },

    /**
     * 停止轮询任务状态
     */
    stopPolling() {
      if (this.pollingInterval !== null) {
        clearInterval(this.pollingInterval);
        this.pollingInterval = null;
        this.isProcessing = false;
      }
    },

    /**
     * 重置状态
     */
    reset() {
      this.stopPolling();
      this.currentTask = null;
      this.error = null;
      this.errorCount = 0; // 重置错误计数
      this.isProcessing = false;
      this.isUploading = false;
      this.isLoading = false;

      console.log('OCR状态已完全重置');
    }
  }
});
