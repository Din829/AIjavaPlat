import { defineStore } from 'pinia';
import { summarizeUrl, type SummarizationRequestData, type SummarizationResponseData } from '../services/summarizationService';

interface SummarizationState {
  summaryResult: string | null;
  isLoading: boolean;
  error: string | null;
  lastSubmittedUrl: string | null;
}

export const useSummarizationStore = defineStore('summarization', {
  state: (): SummarizationState => ({
    summaryResult: null,
    isLoading: false,
    error: null,
    lastSubmittedUrl: null,
  }),

  actions: {
    /**
     * 调用API获取给定URL的网页摘要。
     * @param url 要进行摘要的网页URL。
     */
    async fetchSummary(url: string): Promise<void> {
      if (!url || url.trim() === '') {
        this.error = '请输入有效的URL。';
        return;
      }

      this.isLoading = true;
      this.error = null;
      this.summaryResult = null; // 在新的请求开始时清除旧的结果
      this.lastSubmittedUrl = url;

      try {
        const requestData: SummarizationRequestData = { url };
        const responseData: SummarizationResponseData = await summarizeUrl(requestData);
        
        // 检查后端是否返回了"成功但内容为空/无法提取"的特定提示
        if (responseData.summary === "无法提取网页内容或内容为空。") {
          this.error = responseData.summary; // 将此消息视为错误
          this.summaryResult = null; // 确保摘要结果为空
        } else {
          this.summaryResult = responseData.summary; // 正常的摘要结果
        }
      } catch (err: any) {
        // summarizeUrl服务中已经处理并抛出了一个带有message的Error对象
        let displayError = err.message || '无法生成摘要，请检查URL或稍后再试。';
        
        // 检查是否是由于API Key相关问题导致的底层错误
        if (err.message && (err.message.includes('extracting response') || err.message.includes('API key') || err.message.includes('Unauthorized'))) {
          displayError = 'AI服务调用失败。请检查您的API Token是否正确、有效，并确保账户余额充足。如果问题持续，请尝试更新Token或联系技术支持。';
        }
        
        this.error = displayError;
        this.summaryResult = null; // 确保出错时结果为空
        console.error('Error in fetchSummary action:', err);
      } finally {
        this.isLoading = false;
      }
    },

    /**
     * 重置摘要相关的状态到初始值。
     */
    resetState(): void {
      this.summaryResult = null;
      this.isLoading = false;
      this.error = null;
      this.lastSubmittedUrl = null;
    },
  },

  getters: {
    /**
     * 是否有有效的摘要结果可供显示。
     */
    hasSummaryResult: (state): boolean => {
      return state.summaryResult !== null && state.summaryResult.trim() !== '';
    },

    /**
     * 是否可以提交新的摘要请求 (例如，当前没有正在进行的请求)。
     */
    canSubmit: (state): boolean => {
      return !state.isLoading;
    },
  },
}); 