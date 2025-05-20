import apiClient from './apiClient';
import { AxiosResponse } from 'axios';

/**
 * OCR任务状态
 */
export enum OcrTaskStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

/**
 * OCR上传请求参数
 */
export interface OcrUploadRequest {
  usePypdf2?: boolean;
  useDocling?: boolean;
  useGemini?: boolean;
  forceOcr?: boolean;
  language?: string;
}

/**
 * OCR任务响应
 */
export interface OcrTaskResponse {
  taskId: string;
  status: OcrTaskStatus;
  message?: string;
  result?: any;
  fileName?: string;
  fileSize?: number;
  createdAt?: string;
  completedAt?: string;
}

/**
 * OCR服务
 */
const ocrService = {
  /**
   * 上传文件并处理
   * @param file 文件
   * @param options 选项
   * @returns 任务响应
   */
  async uploadFile(file: File, options: OcrUploadRequest = {}): Promise<OcrTaskResponse> {
    const formData = new FormData();
    formData.append('file', file);
    
    // 添加选项参数
    if (options.usePypdf2 !== undefined) {
      formData.append('usePypdf2', options.usePypdf2.toString());
    }
    if (options.useDocling !== undefined) {
      formData.append('useDocling', options.useDocling.toString());
    }
    if (options.useGemini !== undefined) {
      formData.append('useGemini', options.useGemini.toString());
    }
    if (options.forceOcr !== undefined) {
      formData.append('forceOcr', options.forceOcr.toString());
    }
    if (options.language) {
      formData.append('language', options.language);
    }
    
    const response: AxiosResponse<OcrTaskResponse> = await apiClient.post(
      '/api/ocr/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    );
    
    return response.data;
  },
  
  /**
   * 获取任务状态
   * @param taskId 任务ID
   * @returns 任务响应
   */
  async getTaskStatus(taskId: string): Promise<OcrTaskResponse> {
    const response: AxiosResponse<OcrTaskResponse> = await apiClient.get(
      `/api/ocr/status/${taskId}`
    );
    
    return response.data;
  },
  
  /**
   * 获取任务结果
   * @param taskId 任务ID
   * @returns 任务响应
   */
  async getTaskResult(taskId: string): Promise<OcrTaskResponse> {
    const response: AxiosResponse<OcrTaskResponse> = await apiClient.get(
      `/api/ocr/result/${taskId}`
    );
    
    return response.data;
  },
  
  /**
   * 获取用户的所有OCR任务
   * @returns 任务响应数组
   */
  async getUserTasks(): Promise<OcrTaskResponse[]> {
    const response: AxiosResponse<OcrTaskResponse[]> = await apiClient.get(
      '/api/ocr/tasks'
    );
    
    return response.data;
  }
};

export default ocrService;
