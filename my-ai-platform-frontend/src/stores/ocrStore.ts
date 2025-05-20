import { defineStore } from 'pinia';
import ocrService from '../services/ocrService';
import type { OcrUploadRequest, OcrTaskResponse } from '../types/ocr';
import { OcrTaskStatus } from '../types/ocr';
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
    pollingInterval: null
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

        // 获取任务状态
        await this.getTaskStatus(taskId);
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
    }
  }
});
