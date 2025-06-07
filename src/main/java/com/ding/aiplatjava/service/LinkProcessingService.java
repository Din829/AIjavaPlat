package com.ding.aiplatjava.service;

import com.ding.aiplatjava.dto.LinkProcessRequestDto;
import com.ding.aiplatjava.dto.LinkProcessResponseDto;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 链接处理服务接口
 * 提供统一的链接处理功能，支持网页摘要和视频转写
 */
public interface LinkProcessingService {

    /**
     * 处理链接（网页或视频）
     * 自动识别链接类型并进行相应处理
     * 
     * @param requestDto 链接处理请求DTO
     * @param userId 用户ID
     * @return 链接处理响应DTO，包含任务ID和初始状态
     */
    LinkProcessResponseDto processLink(LinkProcessRequestDto requestDto, Long userId);

    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（用于安全检查）
     * @return 任务状态和基本信息
     */
    LinkProcessResponseDto getTaskStatus(String taskId, Long userId);

    /**
     * 获取任务结果
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（用于安全检查）
     * @return 完整的任务结果
     */
    LinkProcessResponseDto getTaskResult(String taskId, Long userId);

    /**
     * 获取用户的所有任务
     * 
     * @param userId 用户ID
     * @return 用户的任务列表
     */
    List<LinkProcessResponseDto> getUserTasks(Long userId);

    /**
     * 删除用户的任务
     * 
     * @param taskId 任务ID
     * @param userId 用户ID（用于安全检查）
     * @return 是否删除成功
     */
    boolean deleteTask(String taskId, Long userId);

    /**
     * 异步处理链接
     * 内部方法，用于异步执行实际的处理逻辑
     * 
     * @param taskId 任务ID
     * @param requestDto 处理请求
     * @param userId 用户ID
     * @return 异步处理结果
     */
    CompletableFuture<Void> processLinkAsync(String taskId, LinkProcessRequestDto requestDto, Long userId);

    /**
     * 检查链接处理相关微服务的健康状态
     *
     * @return 包含各微服务健康状态的Map
     */
    java.util.Map<String, Object> checkServiceHealth();
}
