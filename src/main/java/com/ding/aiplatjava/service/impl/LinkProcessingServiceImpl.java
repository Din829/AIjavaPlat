package com.ding.aiplatjava.service.impl;

import com.ding.aiplatjava.dto.LinkProcessRequestDto;
import com.ding.aiplatjava.dto.LinkProcessResponseDto;
import com.ding.aiplatjava.entity.VideoTranscriptionTask;
import com.ding.aiplatjava.mapper.VideoTranscriptionTaskMapper;
import com.ding.aiplatjava.service.LinkAnalysisService;
import com.ding.aiplatjava.service.LinkProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 链接处理服务实现类
 * 提供统一的链接处理功能，支持网页摘要和视频转写
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkProcessingServiceImpl implements LinkProcessingService {

    private final VideoTranscriptionTaskMapper taskMapper;
    private final LinkAnalysisService linkAnalysisService;

    @Override
    public LinkProcessResponseDto processLink(LinkProcessRequestDto requestDto, Long userId) {
        try {
            log.info("开始处理链接: {}, 用户ID: {}", requestDto.getUrl(), userId);

            // 1. 生成任务ID
            String taskId = UUID.randomUUID().toString();

            // 2. 检测链接类型
            String contentType = linkAnalysisService.detectContentType(requestDto.getUrl());
            log.info("检测到内容类型: {}", contentType);

            // 3. 创建任务记录
            VideoTranscriptionTask task = VideoTranscriptionTask.builder()
                    .taskId(taskId)
                    .userId(userId)
                    .url(requestDto.getUrl())
                    .contentType(contentType)
                    .status(VideoTranscriptionTask.Status.PENDING)
                    .language(requestDto.getLanguage())
                    .customPrompt(requestDto.getCustomPrompt())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 4. 保存到数据库
            int result = taskMapper.insert(task);
            if (result <= 0) {
                throw new RuntimeException("保存任务到数据库失败");
            }

            log.info("任务创建成功，任务ID: {}, 内容类型: {}", taskId, contentType);

            // 5. 异步处理任务
            processLinkAsync(taskId, requestDto, userId);

            // 6. 返回初始响应
            return LinkProcessResponseDto.builder()
                    .taskId(taskId)
                    .status(VideoTranscriptionTask.Status.PENDING)
                    .contentType(contentType)
                    .url(requestDto.getUrl())
                    .message("任务已创建，正在处理中...")
                    .createdAt(task.getCreatedAt())
                    .build();

        } catch (Exception e) {
            log.error("处理链接时发生错误: {}", requestDto.getUrl(), e);
            throw new RuntimeException("处理链接失败: " + e.getMessage(), e);
        }
    }

    @Override
    public LinkProcessResponseDto getTaskStatus(String taskId, Long userId) {
        try {
            log.debug("获取任务状态: {}, 用户ID: {}", taskId, userId);

            VideoTranscriptionTask task = taskMapper.selectByTaskIdAndUserId(taskId, userId);
            if (task == null) {
                throw new RuntimeException("任务不存在或无权访问");
            }

            return convertToResponseDto(task, false); // 不包含详细结果

        } catch (Exception e) {
            log.error("获取任务状态时发生错误: {}", taskId, e);
            throw new RuntimeException("获取任务状态失败: " + e.getMessage(), e);
        }
    }

    @Override
    public LinkProcessResponseDto getTaskResult(String taskId, Long userId) {
        try {
            log.debug("获取任务结果: {}, 用户ID: {}", taskId, userId);

            VideoTranscriptionTask task = taskMapper.selectByTaskIdAndUserId(taskId, userId);
            if (task == null) {
                throw new RuntimeException("任务不存在或无权访问");
            }

            return convertToResponseDto(task, true); // 包含详细结果

        } catch (Exception e) {
            log.error("获取任务结果时发生错误: {}", taskId, e);
            throw new RuntimeException("获取任务结果失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<LinkProcessResponseDto> getUserTasks(Long userId) {
        try {
            log.debug("获取用户任务列表: {}", userId);

            List<VideoTranscriptionTask> tasks = taskMapper.selectByUserId(userId);
            return tasks.stream()
                    .map(task -> convertToResponseDto(task, false))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户任务列表时发生错误: {}", userId, e);
            throw new RuntimeException("获取任务列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteTask(String taskId, Long userId) {
        try {
            log.info("删除任务: {}, 用户ID: {}", taskId, userId);

            int result = taskMapper.deleteByTaskIdAndUserId(taskId, userId);
            boolean success = result > 0;

            log.info("删除任务结果: {}", success ? "成功" : "失败");
            return success;

        } catch (Exception e) {
            log.error("删除任务时发生错误: {}", taskId, e);
            return false;
        }
    }

    @Override
    @Async
    public CompletableFuture<Void> processLinkAsync(String taskId, LinkProcessRequestDto requestDto, Long userId) {
        try {
            log.info("开始异步处理任务: {}", taskId);

            // 更新状态为处理中
            taskMapper.updateStatus(taskId, VideoTranscriptionTask.Status.PROCESSING);

            // 根据内容类型进行不同的处理
            String contentType = linkAnalysisService.detectContentType(requestDto.getUrl());

            if ("VIDEO".equals(contentType)) {
                processVideoLink(taskId, requestDto);
            } else {
                processWebPageLink(taskId, requestDto);
            }

            log.info("异步处理任务完成: {}", taskId);

        } catch (Exception e) {
            log.error("异步处理任务时发生错误: {}", taskId, e);
            // 更新错误状态
            taskMapper.updateError(taskId, "处理失败: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 处理视频链接
     * 
     * @param taskId 任务ID
     * @param requestDto 请求DTO
     */
    private void processVideoLink(String taskId, LinkProcessRequestDto requestDto) {
        try {
            log.info("开始处理视频链接: {}", requestDto.getUrl());

            // 1. 提取视频元数据
            Map<String, Object> metadata = linkAnalysisService.extractVideoMetadata(requestDto.getUrl());
            
            // 2. 更新视频元数据
            taskMapper.updateVideoMetadata(
                taskId,
                (String) metadata.get("title"),
                (String) metadata.get("description"),
                (Integer) metadata.get("duration")
            );

            // 3. 这里后续会集成视频处理微服务和Whisper转写
            // 目前先模拟处理结果
            String mockResult = "视频处理功能正在开发中...";
            String mockSummary = "这是一个模拟的视频总结结果";

            // 4. 更新处理结果
            taskMapper.updateResult(
                taskId,
                "{\"type\":\"video\",\"status\":\"mock\"}",
                mockResult,
                mockSummary,
                LocalDateTime.now()
            );

            log.info("视频链接处理完成: {}", taskId);

        } catch (Exception e) {
            log.error("处理视频链接时发生错误: {}", taskId, e);
            throw e;
        }
    }

    /**
     * 处理网页链接
     * 
     * @param taskId 任务ID
     * @param requestDto 请求DTO
     */
    private void processWebPageLink(String taskId, LinkProcessRequestDto requestDto) {
        try {
            log.info("开始处理网页链接: {}", requestDto.getUrl());

            // 1. 提取网页标题
            String title = linkAnalysisService.extractWebPageTitle(requestDto.getUrl());

            // 2. 这里后续会集成现有的网页摘要功能
            // 目前先模拟处理结果
            String mockSummary = "这是一个模拟的网页摘要结果，标题: " + title;

            // 3. 更新处理结果
            taskMapper.updateResult(
                taskId,
                "{\"type\":\"webpage\",\"title\":\"" + title + "\"}",
                null, // 网页没有转写文本
                mockSummary,
                LocalDateTime.now()
            );

            log.info("网页链接处理完成: {}", taskId);

        } catch (Exception e) {
            log.error("处理网页链接时发生错误: {}", taskId, e);
            throw e;
        }
    }

    /**
     * 将任务实体转换为响应DTO
     * 
     * @param task 任务实体
     * @param includeDetailedResult 是否包含详细结果
     * @return 响应DTO
     */
    private LinkProcessResponseDto convertToResponseDto(VideoTranscriptionTask task, boolean includeDetailedResult) {
        LinkProcessResponseDto.LinkProcessResponseDtoBuilder builder = LinkProcessResponseDto.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .contentType(task.getContentType())
                .url(task.getUrl())
                .videoTitle(task.getVideoTitle())
                .videoDescription(task.getVideoDescription())
                .videoDuration(task.getVideoDuration())
                .summaryText(task.getSummaryText())
                .errorMessage(task.getErrorMessage())
                .createdAt(task.getCreatedAt())
                .completedAt(task.getCompletedAt());

        // 根据状态设置消息
        String message = switch (task.getStatus()) {
            case "PENDING" -> "任务等待处理中...";
            case "PROCESSING" -> "任务正在处理中...";
            case "COMPLETED" -> "任务处理完成";
            case "FAILED" -> "任务处理失败";
            default -> "未知状态";
        };
        builder.message(message);

        // 如果需要详细结果且任务已完成
        if (includeDetailedResult && "COMPLETED".equals(task.getStatus())) {
            builder.transcriptionText(task.getTranscriptionText());
            // 这里可以解析resultJson并设置detailedResult
            builder.detailedResult(task.getResultJson());
        }

        return builder.build();
    }
}
