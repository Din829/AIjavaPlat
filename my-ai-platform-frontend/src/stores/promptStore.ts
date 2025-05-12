import { defineStore } from 'pinia';
import {
  getPrompts,
  getPromptById,
  createPrompt,
  updatePrompt,
  deletePrompt,
  type Prompt,
  type PromptData
} from '../services/promptService';
import { useMessage } from 'naive-ui'; // 导入 useMessage

// 在 store 外部获取 message 实例，如果在 action 内部需要，可以在 setup store 中注入
// 或者像 authStore 那样在 action 内部调用 use...()，但可能影响测试
// 这里暂时不在 action 内部直接使用 message，UI 层可以通过监听 error 状态来显示

// --- Store 定义 ---
export const usePromptStore = defineStore('prompt', {
  state: () => ({
    prompts: [] as Prompt[],
    currentPrompt: null as Prompt | null,
    loading: false,
    error: null as string | null,
  }),

  actions: {
    // --- 内部辅助方法 (可选) ---
    setLoading(isLoading: boolean) {
      this.loading = isLoading;
    },
    setError(errorMessage: string | null) {
      this.error = errorMessage;
    },
    setPrompts(promptList: Prompt[]) {
      this.prompts = promptList;
    },
    setCurrentPrompt(prompt: Prompt | null) {
      this.currentPrompt = prompt;
    },
    // 移除列表中的某个 prompt (用于删除成功后)
    removePromptFromList(id: number | string) {
      this.prompts = this.prompts.filter(p => p.id !== id);
    },

    // --- 主要 Actions ---
    async fetchPrompts() {
      this.setLoading(true);
      this.setError(null);
      try {
        const data = await getPrompts();
        this.setPrompts(data);
      } catch (err: any) {
        this.setError(err.message || '获取 Prompt 列表时发生未知错误');
        this.setPrompts([]); // 出错时清空列表
      } finally {
        this.setLoading(false);
      }
    },

    async fetchPromptById(id: number | string) {
      this.setLoading(true);
      this.setError(null);
      this.setCurrentPrompt(null); // 先清空
      try {
        const data = await getPromptById(id);
        this.setCurrentPrompt(data);
      } catch (err: any) {
        this.setError(err.message || '获取单个 Prompt 时发生未知错误');
      } finally {
        this.setLoading(false);
      }
    },

    async addPrompt(data: PromptData) {
      this.setLoading(true);
      this.setError(null);
      try {
        await createPrompt(data);
        // 成功后刷新列表
        await this.fetchPrompts();
        // 可以在这里返回成功状态或抛出成功事件，通知 UI
      } catch (err: any) {
        this.setError(err.message || '创建 Prompt 时发生未知错误');
        throw err; // 将错误向上抛出，方便 UI 层处理
      }
       finally {
        this.setLoading(false);
      }
    },

    async editPrompt(id: number | string, data: PromptData) {
      this.setLoading(true);
      this.setError(null);
      try {
        await updatePrompt(id, data);
        // 成功后刷新列表
        await this.fetchPrompts();
        // 也可以考虑只更新 currentPrompt (如果需要)
        // const updated = await getPromptById(id);
        // this.setCurrentPrompt(updated);
      } catch (err: any) {
        this.setError(err.message || '更新 Prompt 时发生未知错误');
        throw err;
      } finally {
        this.setLoading(false);
      }
    },

    async removePrompt(id: number | string) {
      this.setLoading(true);
      this.setError(null);
      try {
        await deletePrompt(id);
        // 成功后从列表中移除 (或者重新 fetch)
        this.removePromptFromList(id);
        // 如果删除的是当前查看的 prompt，也清空 currentPrompt
        if (this.currentPrompt?.id === id) {
          this.setCurrentPrompt(null);
        }
      } catch (err: any) {
        this.setError(err.message || '删除 Prompt 时发生未知错误');
        throw err;
      } finally {
        this.setLoading(false);
      }
    },

    resetState() {
      this.prompts = [];
      this.currentPrompt = null;
      this.loading = false;
      this.error = null;
    },
  },
}); 