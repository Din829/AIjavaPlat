/**
 * Token状态管理
 * 使用Pinia管理API Token相关的状态
 * 
 * 主要功能：
 * 1. 存储和管理用户的API Token列表
 * 2. 提供Token的CRUD操作
 * 3. 管理Token相关的加载状态和错误信息
 */

import { defineStore } from 'pinia';
import { 
  getTokens, 
  createToken, 
  deleteToken, 
  type Token, 
  type CreateTokenRequest 
} from '../services/tokenService';
import { message } from '../services/messageService';

/**
 * 定义名为'token'的Pinia store
 */
export const useTokenStore = defineStore('token', {
  /**
   * 状态：定义store的响应式数据属性
   */
  state: () => ({
    /**
     * Token列表
     * 初始为空数组，在fetchTokens action中加载
     */
    tokens: [] as Token[],
    
    /**
     * 加载状态
     * 用于控制UI中的加载指示器
     */
    loading: false,
    
    /**
     * 创建Token的加载状态
     * 用于控制创建Token按钮的禁用状态和加载指示
     */
    creatingToken: false,
    
    /**
     * 删除Token的加载状态
     * 存储正在删除的Token ID，用于在UI中显示对应的加载状态
     */
    deletingTokenId: null as number | null,
    
    /**
     * 错误信息
     * 存储最近一次操作的错误信息
     */
    error: null as string | null,
  }),

  /**
   * Getters：类似于Vue组件的计算属性
   * 用于从state派生出一些值
   */
  getters: {
    /**
     * 获取按创建时间排序的Token列表
     * 最新创建的Token排在前面
     */
    sortedTokens: (state) => {
      return [...state.tokens].sort((a, b) => {
        // 按创建时间降序排序（新的在前）
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      });
    },
    
    /**
     * 检查是否有特定提供商的Token
     * @param provider Token提供商
     * @returns 是否存在该提供商的Token
     */
    hasProviderToken: (state) => (provider: string) => {
      return state.tokens.some(token => token.provider === provider);
    },
    
    /**
     * 获取Token总数
     */
    tokenCount: (state) => state.tokens.length,
  },

  /**
   * Actions：定义可以修改state的方法
   * 包括同步和异步操作
   */
  actions: {
    /**
     * 获取用户的所有Token
     * @returns Promise<void>
     */
    async fetchTokens(): Promise<void> {
      // 设置加载状态
      this.loading = true;
      this.error = null;
      
      try {
        // 调用API获取Token列表
        const tokens = await getTokens();
        this.tokens = tokens;
      } catch (error) {
        // 处理错误
        console.error('[tokenStore] Failed to fetch tokens:', error);
        this.error = '获取Token列表失败';
        message.error(this.error);
      } finally {
        // 无论成功或失败，都结束加载状态
        this.loading = false;
      }
    },
    
    /**
     * 创建新的Token
     * @param tokenData 创建Token的请求数据
     * @returns Promise<Token | null> 创建成功返回Token，失败返回null
     */
    async addToken(tokenData: CreateTokenRequest): Promise<Token | null> {
      // 设置创建Token的加载状态
      this.creatingToken = true;
      this.error = null;
      
      try {
        // 调用API创建Token
        const newToken = await createToken(tokenData);
        
        // 将新Token添加到列表中
        this.tokens.push(newToken);
        
        // 显示成功消息
        message.success('Token创建成功');
        
        return newToken;
      } catch (error) {
        // 处理错误
        console.error('[tokenStore] Failed to create token:', error);
        this.error = '创建Token失败';
        message.error(this.error);
        return null;
      } finally {
        // 无论成功或失败，都结束创建Token的加载状态
        this.creatingToken = false;
      }
    },
    
    /**
     * 删除指定ID的Token
     * @param id Token ID
     * @returns Promise<boolean> 删除成功返回true，失败返回false
     */
    async removeToken(id: number): Promise<boolean> {
      // 设置删除Token的加载状态
      this.deletingTokenId = id;
      this.error = null;
      
      try {
        // 调用API删除Token
        await deleteToken(id);
        
        // 从列表中移除被删除的Token
        this.tokens = this.tokens.filter(token => token.id !== id);
        
        // 显示成功消息
        message.success('Token删除成功');
        
        return true;
      } catch (error) {
        // 处理错误
        console.error(`[tokenStore] Failed to delete token with ID ${id}:`, error);
        this.error = '删除Token失败';
        message.error(this.error);
        return false;
      } finally {
        // 无论成功或失败，都结束删除Token的加载状态
        this.deletingTokenId = null;
      }
    },
    
    /**
     * 重置状态
     * 清空Token列表和错误信息，重置加载状态
     * 通常在用户登出时调用
     */
    resetState() {
      this.tokens = [];
      this.loading = false;
      this.creatingToken = false;
      this.deletingTokenId = null;
      this.error = null;
    },
  },
});
