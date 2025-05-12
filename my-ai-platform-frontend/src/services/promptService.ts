import apiClient from './apiClient';

// --- 类型定义 ---

/**
 * Prompt 数据接口 (对应后端的 PromptDto 或 Prompt 实体)
 * 注意：时间戳可能是字符串 (来自 DTO) 或 Date 对象 (如果直接解析实体)
 *       但为简单起见，我们统一处理为字符串或 null。
 */
export interface Prompt {
  id: number | string;
  title: string;
  content: string;
  category: string | null; // 分类可以是 null
  createdAt?: string | null; // DTO 是格式化字符串
  updatedAt?: string | null; // DTO 是格式化字符串
  // userId?: number | string; // 实体包含，DTO 不含，通常前端不需要
}

/**
 * 用于创建或更新 Prompt 时发送的数据接口。
 * 只包含用户需要编辑的字段。
 */
export interface PromptData {
  title: string;
  content: string;
  category: string | null;
}

// --- 服务方法 ---

/**
 * 获取当前用户的所有 Prompts。
 * @returns {Promise<Prompt[]>} 一个 Promise，解析为 Prompt 数组。
 */
export const getPrompts = async (): Promise<Prompt[]> => {
  console.log('[promptService] Fetching all prompts...');
  try {
    // 后端 GET /api/prompts 返回 List<PromptDto>
    const response = await apiClient.get<Prompt[]>('/api/prompts');
    console.log('[promptService] Fetched prompts:', response.data);
    return response.data;
  } catch (error) {
    console.error('[promptService] Failed to fetch prompts:', error);
    throw new Error('获取 Prompt 列表失败');
  }
};

/**
 * 根据 ID 获取单个 Prompt。
 * @param {number | string} id - Prompt ID。
 * @returns {Promise<Prompt>} 一个 Promise，解析为单个 Prompt 对象。
 */
export const getPromptById = async (id: number | string): Promise<Prompt> => {
  console.log(`[promptService] Fetching prompt by ID: ${id}`);
  try {
    // 后端 GET /api/prompts/{id} 返回 Prompt 实体
    const response = await apiClient.get<Prompt>(`/api/prompts/${id}`);
    console.log('[promptService] Fetched prompt:', response.data);
    return response.data;
  } catch (error) {
    console.error(`[promptService] Failed to fetch prompt ${id}:`, error);
    throw new Error(`获取 Prompt (ID: ${id}) 失败`);
  }
};

/**
 * 创建新的 Prompt。
 * @param {PromptData} data - 要创建的 Prompt 数据 (title, content, category)。
 * @returns {Promise<Prompt>} 一个 Promise，解析为创建后的 Prompt 对象 (后端返回实体)。
 */
export const createPrompt = async (data: PromptData): Promise<Prompt> => {
  console.log('[promptService] Creating new prompt:', data);
  try {
    // 后端 POST /api/prompts 请求体接收 Prompt，返回创建后的 Prompt 实体
    const response = await apiClient.post<Prompt>('/api/prompts', data);
    console.log('[promptService] Created prompt:', response.data);
    return response.data;
  } catch (error) {
    console.error('[promptService] Failed to create prompt:', error);
    throw new Error('创建 Prompt 失败');
  }
};

/**
 * 更新现有的 Prompt。
 * @param {number | string} id - 要更新的 Prompt ID。
 * @param {PromptData} data - 更新后的 Prompt 数据 (title, content, category)。
 * @returns {Promise<Prompt>} 一个 Promise，解析为更新后的 Prompt 对象 (后端返回实体)。
 */
export const updatePrompt = async (id: number | string, data: PromptData): Promise<Prompt> => {
  console.log(`[promptService] Updating prompt ${id}:`, data);
  try {
    // 后端 PUT /api/prompts/{id} 请求体接收 Prompt，返回更新后的 Prompt 实体
    const response = await apiClient.put<Prompt>(`/api/prompts/${id}`, data);
    console.log('[promptService] Updated prompt:', response.data);
    return response.data;
  } catch (error) {
    console.error(`[promptService] Failed to update prompt ${id}:`, error);
    throw new Error(`更新 Prompt (ID: ${id}) 失败`);
  }
};

/**
 * 删除 Prompt。
 * @param {number | string} id - 要删除的 Prompt ID。
 * @returns {Promise<void>} 一个 Promise，在删除成功时 resolve。
 */
export const deletePrompt = async (id: number | string): Promise<void> => {
  console.log(`[promptService] Deleting prompt ${id}`);
  try {
    // 后端 DELETE /api/prompts/{id} 成功返回 204 No Content
    await apiClient.delete(`/api/prompts/${id}`);
    console.log(`[promptService] Deleted prompt ${id}`);
  } catch (error) {
    console.error(`[promptService] Failed to delete prompt ${id}:`, error);
    throw new Error(`删除 Prompt (ID: ${id}) 失败`);
  }
}; 