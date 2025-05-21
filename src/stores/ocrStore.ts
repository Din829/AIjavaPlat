import { defineStore } from 'pinia';
import ocrService, { OcrTaskStatus } from '../services/ocrService';
import type { OcrUploadRequest, OcrTaskResponse } from '../services/ocrService';
import { useMessageStore } from './messageStore';

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
      const messageStore = useMessageStore();
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
        this.currentTask = response;

        // 开始轮询任务状态
        this.startPolling(response.taskId);

        messageStore.success('文件上传成功，正在处理中...');
        return response;
      } catch (error: any) {
        this.error = error.message || '上传文件失败';
        messageStore.error(this.error);
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
      const messageStore = useMessageStore();
      this.isLoading = true;

      try {
        const response = await ocrService.getTaskStatus(taskId);
        this.currentTask = response;

        // 如果任务已完成或失败，停止轮询
        if (response.status === OcrTaskStatus.COMPLETED ||
            response.status === OcrTaskStatus.FAILED) {
          this.stopPolling();

          if (response.status === OcrTaskStatus.COMPLETED) {
            messageStore.success('OCR处理完成');
          } else if (response.status === OcrTaskStatus.FAILED) {
            messageStore.error(`OCR处理失败: ${response.message || '未知错误'}`);
          }
        }

        return response;
      } catch (error: any) {
        this.error = error.message || '获取任务状态失败';
        messageStore.error(this.error);
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
      const messageStore = useMessageStore();
      this.isLoading = true;

      try {
        const response = await ocrService.getTaskResult(taskId);
        this.currentTask = response;
        return response;
      } catch (error: any) {
        this.error = error.message || '获取任务结果失败';
        messageStore.error(this.error);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    /**
     * 获取用户的所有OCR任务
     */
    async getUserTasks() {
      const messageStore = useMessageStore();
      this.isLoading = true;

      try {
        const response = await ocrService.getUserTasks();
        this.tasks = response;
        return response;
      } catch (error: any) {
        this.error = error.message || '获取任务列表失败';
        messageStore.error(this.error);
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

      // 开始新的轮询
      this.isProcessing = true;
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
          // 成功获取状态后重置错误计数
          this.errorCount = 0;
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
      }, 3000); // 每3秒轮询一次
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
    }
  }
});
