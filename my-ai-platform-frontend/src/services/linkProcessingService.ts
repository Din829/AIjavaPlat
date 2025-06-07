/**
 * 链接处理服务
 * 提供视频转写和网页摘要的API调用功能
 * 
 * @author Ding
 * @since 2024-12-28
 */

import apiClient from './apiClient';
import type { 
  LinkProcessRequest, 
  LinkAnalysisResponse, 
  TaskCreateResponse, 
  TaskDetailResponse,
  TaskListResponse 
} from '../types/linkProcessing';

/**
 * 链接处理服务类
 */
export class LinkProcessingService {
  
  /**
   * 分析链接类型和支持情况
   * 
   * @param url 要分析的链接
   * @returns 链接分析结果
   */
  static async analyzeLink(url: string): Promise<LinkAnalysisResponse> {
    try {
      // console.log('[Debug] apiClient.defaults.baseURL before /analyze call:', apiClient.defaults.baseURL);
      const response = await apiClient.post('/api/link-processing/analyze', { url });
      return response.data;
    } catch (error) {
      console.error('链接分析失败:', error);
      throw error;
    }
  }

  /**
   * 创建链接处理任务
   * 
   * @param request 处理请求参数
   * @returns 任务创建结果
   */
  static async createTask(request: LinkProcessRequest): Promise<TaskCreateResponse> {
    try {
      // console.log('[Debug] apiClient.defaults.baseURL before /process call:', apiClient.defaults.baseURL);
      const response = await apiClient.post('/api/link-processing/process', request);
      return response.data;
    } catch (error) {
      console.error('创建处理任务失败:', error);
      throw error;
    }
  }

  /**
   * 获取任务详情
   * 
   * @param taskId 任务ID
   * @returns 任务详情
   */
  static async getTaskDetail(taskId: string): Promise<TaskDetailResponse> {
    try {
      // console.log('[Debug] apiClient.defaults.baseURL before /task/:taskId call:', apiClient.defaults.baseURL);
      const response = await apiClient.get(`/api/link-processing/task/${taskId}`);
      return response.data;
    } catch (error) {
      console.error('获取任务详情失败:', error);
      throw error;
    }
  }

  /**
   * 获取用户的任务列表
   * 
   * @param page 页码（从0开始）
   * @param size 每页大小
   * @returns 任务列表
   */
  static async getTaskList(page: number = 0, size: number = 10): Promise<TaskListResponse> {
    try {
      console.log('[Debug] apiClient.defaults.baseURL before /tasks call:', apiClient.defaults.baseURL);
      const response = await apiClient.get('/api/link-processing/tasks', {
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      console.error('获取任务列表失败:', error);
      throw error;
    }
  }

  /**
   * 删除任务
   * 
   * @param taskId 任务ID
   */
  static async deleteTask(taskId: string): Promise<void> {
    try {
      // console.log('[Debug] apiClient.defaults.baseURL before delete /task/:taskId call:', apiClient.defaults.baseURL);
      await apiClient.delete(`/api/link-processing/task/${taskId}`);
    } catch (error) {
      console.error('删除任务失败:', error);
      throw error;
    }
  }

  /**
   * 轮询任务状态直到完成
   * 
   * @param taskId 任务ID
   * @param onProgress 进度回调函数
   * @param maxAttempts 最大轮询次数
   * @param interval 轮询间隔（毫秒）
   * @param abortSignal 中断信号，用于取消轮询
   * @returns 最终任务详情
   */
  static async pollTaskStatus(
    taskId: string,
    onProgress?: (task: TaskDetailResponse) => void,
    maxAttempts: number = 60,
    interval: number = 5000,
    abortSignal?: AbortSignal
  ): Promise<TaskDetailResponse> {
    let attempts = 0;
    
    while (attempts < maxAttempts) {
      // 检查是否已被中断
      if (abortSignal?.aborted) {
        throw new Error('轮询已被中断');
      }
      
      try {
        const task = await this.getTaskDetail(taskId);
        
        // 再次检查是否被中断（在网络请求后）
        if (abortSignal?.aborted) {
          throw new Error('轮询已被中断');
        }
        
        // 调用进度回调
        if (onProgress) {
          try {
            onProgress(task);
          } catch (callbackError) {
            console.warn('进度回调执行失败:', callbackError);
            // 如果回调失败，继续轮询但不抛出错误
          }
        }
        
        // 检查任务是否完成
        if (task.status === 'COMPLETED' || task.status === 'FAILED') {
          return task;
        }
        
        // 等待指定间隔后继续轮询
        await new Promise((resolve, reject) => {
          const timeoutId = setTimeout(resolve, interval);
          
          // 如果有中断信号，监听中断事件
          if (abortSignal) {
            const abortHandler = () => {
              clearTimeout(timeoutId);
              reject(new Error('轮询已被中断'));
            };
            
            if (abortSignal.aborted) {
              clearTimeout(timeoutId);
              reject(new Error('轮询已被中断'));
              return;
            }
            
            abortSignal.addEventListener('abort', abortHandler, { once: true });
            
            // 清理监听器
            setTimeout(() => {
              abortSignal.removeEventListener('abort', abortHandler);
            }, interval + 100);
          }
        });
        
        attempts++;
        
      } catch (error) {
        // 如果是中断错误，直接抛出
        if (error instanceof Error && error.message === '轮询已被中断') {
          throw error;
        }
        
        console.error(`轮询任务状态失败 (第${attempts + 1}次):`, error);
        attempts++;
        
        // 检查是否被中断
        if (abortSignal?.aborted) {
          throw new Error('轮询已被中断');
        }
        
        // 如果是网络错误，继续重试
        if (attempts < maxAttempts) {
          await new Promise((resolve, reject) => {
            const timeoutId = setTimeout(resolve, interval);
            
            if (abortSignal) {
              const abortHandler = () => {
                clearTimeout(timeoutId);
                reject(new Error('轮询已被中断'));
              };
              
              if (abortSignal.aborted) {
                clearTimeout(timeoutId);
                reject(new Error('轮询已被中断'));
                return;
              }
              
              abortSignal.addEventListener('abort', abortHandler, { once: true });
              
              setTimeout(() => {
                abortSignal.removeEventListener('abort', abortHandler);
              }, interval + 100);
            }
          });
        } else {
          throw error;
        }
      }
    }
    
    throw new Error(`任务轮询超时: ${taskId}`);
  }

  /**
   * 检查服务健康状态
   * 
   * @returns 服务状态信息
   */
  static async checkServiceHealth(): Promise<{
    videoService: boolean;
    whisperService: boolean;
    message: string;
  }> {
    try {
      console.log('[Debug] apiClient.defaults.baseURL before /health call:', apiClient.defaults.baseURL);
      const response = await apiClient.get('/api/link-processing/health');
      return response.data;
    } catch (error) {
      console.error('检查服务健康状态失败:', error);
      return {
        videoService: false,
        whisperService: false,
        message: '服务状态检查失败'
      };
    }
  }
}

// 导出默认实例
export default LinkProcessingService;
