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
        this.summaryResult = responseData.summary;
      } catch (err: any) {
        // summarizeUrl服务中已经处理并抛出了一个带有message的Error对象
        this.error = err.message || '无法生成摘要，请检查URL或稍后再试。';
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