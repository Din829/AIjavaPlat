/**
 * Token服务
 * 提供API Token的CRUD操作，与后端API交互
 * 
 * 主要功能：
 * 1. 获取用户的API Token列表
 * 2. 创建新的API Token
 * 3. 删除API Token
 */

import apiClient from './apiClient';

/**
 * API Token提供商枚举
 * 定义支持的AI服务提供商类型
 */
export enum TokenProvider {
  OPENAI = 'OPENAI',
  AZURE_OPENAI = 'AZURE_OPENAI',
  // 可以根据需要添加更多提供商
}

/**
 * API Token接口
 * 与后端TokenDto对应
 */
export interface Token {
  id: number;           // Token ID
  provider: TokenProvider; // 提供商类型
  name: string;         // Token名称/描述
  tokenValue: string;   // Token值（可能是掩码处理后的）
  createdAt: string;    // 创建时间
  updatedAt: string;    // 更新时间
}

/**
 * 创建Token请求接口
 * 与后端CreateTokenRequestDto对应
 */
export interface CreateTokenRequest {
  provider: TokenProvider; // 提供商类型
  name: string;         // Token名称/描述
  tokenValue: string;   // Token值
}

/**
 * 获取当前用户的所有API Token
 * @returns Promise<Token[]> Token列表
 */
export const getTokens = async (): Promise<Token[]> => {
  try {
    // 调用后端API获取Token列表
    const response = await apiClient.get<Token[]>('/api/tokens');
    return response.data;
  } catch (error) {
    console.error('[tokenService] Failed to fetch tokens:', error);
    throw error; // 将错误向上抛出，由调用方处理
  }
};

/**
 * 创建新的API Token
 * @param tokenData 创建Token的请求数据
 * @returns Promise<Token> 创建成功的Token
 */
export const createToken = async (tokenData: CreateTokenRequest): Promise<Token> => {
  try {
    // 调用后端API创建Token
    const response = await apiClient.post<Token>('/api/tokens', tokenData);
    return response.data;
  } catch (error) {
    console.error('[tokenService] Failed to create token:', error);
    throw error; // 将错误向上抛出，由调用方处理
  }
};

/**
 * 删除指定ID的API Token
 * @param id Token ID
 * @returns Promise<void>
 */
export const deleteToken = async (id: number): Promise<void> => {
  try {
    // 调用后端API删除Token
    await apiClient.delete(`/api/tokens/${id}`);
  } catch (error) {
    console.error(`[tokenService] Failed to delete token with ID ${id}:`, error);
    throw error; // 将错误向上抛出，由调用方处理
  }
};

/**
 * 获取Token提供商的显示名称
 * @param provider Token提供商枚举值
 * @returns 提供商的中文显示名称
 */
export const getProviderDisplayName = (provider: TokenProvider): string => {
  switch (provider) {
    case TokenProvider.OPENAI:
      return 'OpenAI';
    case TokenProvider.AZURE_OPENAI:
      return 'Azure OpenAI';
    default:
      return provider; // 如果没有特定的显示名称，则返回原始值
  }
};

/**
 * 获取所有支持的Token提供商选项
 * 用于下拉选择框
 * @returns {Array<{label: string, value: TokenProvider}>} 提供商选项数组
 */
export const getProviderOptions = (): Array<{label: string, value: TokenProvider}> => {
  return [
    { label: 'OpenAI', value: TokenProvider.OPENAI },
    { label: 'Azure OpenAI', value: TokenProvider.AZURE_OPENAI },
    // 可以根据需要添加更多选项
  ];
};

/**
 * 对Token值进行掩码处理
 * 只显示前4位和后4位，中间用星号代替
 * @param tokenValue 完整的Token值
 * @returns 掩码处理后的Token值
 */
export const maskTokenValue = (tokenValue: string): string => {
  if (!tokenValue) return '';
  if (tokenValue.length <= 8) return tokenValue; // 如果Token太短，不进行掩码处理
  
  const prefix = tokenValue.substring(0, 4);
  const suffix = tokenValue.substring(tokenValue.length - 4);
  const maskedLength = tokenValue.length - 8;
  const maskedPart = '*'.repeat(Math.min(maskedLength, 20)); // 限制星号数量，避免过长
  
  return `${prefix}${maskedPart}${suffix}`;
};
