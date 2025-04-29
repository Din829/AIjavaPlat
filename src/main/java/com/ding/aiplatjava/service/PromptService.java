package com.ding.aiplatjava.service;

import java.util.List;

import com.ding.aiplatjava.entity.Prompt;

/**
 * Prompt 服务接口
 * 定义 Prompt 相关的业务逻辑操作
 */
public interface PromptService {

    /**
     * 根据ID获取Prompt
     * @param id Prompt ID
     * @param userId 当前用户ID (用于权限校验)
     * @return Prompt实体，如果不存在或无权访问返回null
     */
    Prompt getPromptById(Long id, Long userId);

    /**
     * 获取指定用户的所有Prompt
     * @param userId 用户ID
     * @return Prompt列表
     */
    List<Prompt> getPromptsByUserId(Long userId);

    /**
     * 创建新的Prompt
     * @param prompt 要创建的Prompt实体 (应包含userId, title, content, category)
     * @param userId 当前用户ID (用于设置userId字段)
     * @return 创建成功后的Prompt实体 (包含生成的ID和时间戳)
     */
    Prompt createPrompt(Prompt prompt, Long userId);

    /**
     * 更新现有的Prompt
     * @param id 要更新的Prompt ID
     * @param prompt 包含更新信息的Prompt实体 (允许更新 title, content, category)
     * @param userId 当前用户ID (用于权限校验)
     * @return 更新成功后的Prompt实体，如果不存在或无权访问返回null
     */
    Prompt updatePrompt(Long id, Prompt prompt, Long userId);

    /**
     * 删除Prompt
     * @param id 要删除的Prompt ID
     * @param userId 当前用户ID (用于权限校验)
     * @return 删除成功返回true，否则返回false
     */
    boolean deletePrompt(Long id, Long userId);
} 