import apiClient from './apiClient';

/**
 * 请求后端进行网页摘要所需的数据结构。
 */
export interface SummarizationRequestData {
  url: string;
}

/**
 * 后端返回的网页摘要结果的数据结构。
 */
export interface SummarizationResponseData {
  summary: string;
  // 未来可以扩展，例如包含原始标题、提取的关键词等
  // success?: boolean; // 如果后端也返回布尔类型的成功状态
  // message?: string; // 如果后端在成功或失败时都可能返回消息
}

/**
 * 调用后端API以获取给定URL的网页内容摘要。
 * @param data包含要摘要的URL。
 * @returns 一个Promise，解析为包含摘要结果的对象。
 * @throws 如果API调用失败或返回错误，则抛出错误。
 */
export const summarizeUrl = async (data: SummarizationRequestData): Promise<SummarizationResponseData> => {
  try {
    const response = await apiClient.post<SummarizationResponseData>('/api/summarize', data);
    // apiClient的响应拦截器通常会处理HTTP层面的错误 (4xx, 5xx)
    // 如果后端在2xx响应中通过特定字段表示业务错误 (例如 { success: false, message: '...' })，
    // 这里可以添加额外的检查。但根据当前DTO设计，成功时直接返回摘要。
    return response.data;
  } catch (error: any) {
    // apiClient的错误拦截器应该已经格式化了错误对象，并可能通过messageService显示了通用错误。
    // 这里可以根据需要进一步处理或重新抛出特定的错误类型。
    // 例如，如果错误对象包含后端返回的特定业务错误信息，可以在这里提取并抛出。
    const errorMessage = error.response?.data?.message || error.message || '获取网页摘要失败，请稍后再试。';
    console.error('summarizeUrl API error:', error.response?.data || error);
    // 抛出错误，以便store层可以捕获并更新UI状态
    throw new Error(errorMessage);
  }
};

// 可以根据需要添加其他与摘要相关的服务函数
// 例如: getSummarizationHistory, getSupportedAiProvidersForSummarization 等 