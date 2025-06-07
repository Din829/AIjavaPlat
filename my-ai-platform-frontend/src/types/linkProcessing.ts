/**
 * 链接处理相关的类型定义
 * 用于视频转写和网页摘要功能
 * 
 * @author Ding
 * @since 2024-12-28
 */

/**
 * 链接类型枚举
 */
export enum LinkType {
  VIDEO = 'VIDEO',
  WEBPAGE = 'WEBPAGE',
  UNKNOWN = 'UNKNOWN'
}

/**
 * 任务状态枚举
 */
export enum TaskStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING', 
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED'
}

/**
 * 链接处理请求参数
 */
export interface LinkProcessRequest {
  url: string;
  language?: string;
  customPrompt?: string;
}

/**
 * 链接分析响应
 */
export interface LinkAnalysisResponse {
  linkType: LinkType;
  platform?: string;
  title?: string;
  description?: string;
  isSupported: boolean;
  message?: string;
}

/**
 * 任务创建响应
 */
export interface TaskCreateResponse {
  taskId: string;
  status: TaskStatus;
  linkType: LinkType;
  message?: string;
}

/**
 * 转写分段信息
 */
export interface TranscriptionSegment {
  start: number;
  end: number;
  text: string;
}

/**
 * 转写结果
 */
export interface TranscriptionResult {
  language: string;
  languageProbability: number;
  segments: TranscriptionSegment[];
  fullText: string;
  processingInfo: Record<string, any>;
}

/**
 * 视频元数据
 */
export interface VideoMetadata {
  videoId: string;
  title: string;
  description: string;
  duration: number; // 保持number类型，JavaScript中number已支持浮点数
  platform: string;
}

/**
 * 任务详情响应
 */
export interface TaskDetailResponse {
  taskId: string;
  userId: number;
  url: string;
  linkType: LinkType;
  status: TaskStatus;
  language?: string;
  customPrompt?: string;
  
  // 视频相关字段
  videoTitle?: string;
  videoDescription?: string;
  videoDuration?: number; // 支持浮点数精度的视频时长
  
  // 结果字段
  resultJson?: string;
  transcriptionText?: string;
  summary?: string;
  errorMessage?: string;
  
  // 时间字段
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
}

/**
 * 任务列表响应
 */
export interface TaskListResponse {
  tasks: TaskDetailResponse[];
  total: number;
  page: number;
  size: number;
}

/**
 * 处理进度信息
 */
export interface ProcessingProgress {
  currentStep: number;
  totalSteps: number;
  stepName: string;
  stepDescription: string;
  percentage: number;
  status: 'active' | 'success' | 'error' | 'wait';
}

/**
 * 链接处理状态
 */
export interface LinkProcessingState {
  isProcessing: boolean;
  currentTask?: TaskDetailResponse;
  progress?: ProcessingProgress;
  error?: string;
}

/**
 * 支持的语言选项
 */
export interface LanguageOption {
  label: string;
  value: string;
}

/**
 * 支持的视频平台
 */
export const SUPPORTED_PLATFORMS = [
  'YouTube',
  'Bilibili', 
  'Vimeo',
  'Dailymotion',
  'Twitch'
] as const;

/**
 * 语言选项列表
 */
export const LANGUAGE_OPTIONS: LanguageOption[] = [
  { label: '自动检测', value: 'auto' },
  { label: '中文', value: 'zh' },
  { label: '英文', value: 'en' },
  { label: '日文', value: 'ja' },
  { label: '韩文', value: 'ko' },
  { label: '法文', value: 'fr' },
  { label: '德文', value: 'de' },
  { label: '西班牙文', value: 'es' },
  { label: '俄文', value: 'ru' },
  { label: '混合语言', value: 'mixed' }
];

/**
 * 处理步骤定义
 */
export const PROCESSING_STEPS = {
  VIDEO: [
    { name: 'analyze', description: '分析链接类型' },
    { name: 'download', description: '下载视频和提取元数据' },
    { name: 'transcribe', description: '语音转写处理' },
    { name: 'summarize', description: 'AI总结生成' }
  ],
  WEBPAGE: [
    { name: 'analyze', description: '分析链接类型' },
    { name: 'extract', description: '提取网页内容' },
    { name: 'summarize', description: 'AI摘要生成' }
  ]
} as const;
