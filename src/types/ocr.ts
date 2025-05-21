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
