package com.ding.aiplatjava.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 链接处理响应的数据传输对象 (DTO)
 * 用于返回链接处理任务的状态和结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // 不序列化null值
public class LinkProcessResponseDto {

    /**
     * 任务ID（UUID）
     */
    private String taskId;

    /**
     * 任务状态：PENDING, PROCESSING, COMPLETED, FAILED
     */
    private String status;

    /**
     * 内容类型：WEBPAGE, VIDEO
     */
    private String contentType;

    /**
     * 处理的URL链接
     */
    private String url;

    /**
     * 状态消息或提示信息
     */
    private String message;

    /**
     * 视频标题（仅当contentType为VIDEO时有值）
     */
    private String videoTitle;

    /**
     * 视频描述（仅当contentType为VIDEO时有值）
     */
    private String videoDescription;

    /**
     * 视频时长（秒）（仅当contentType为VIDEO时有值）
     */
    private Integer videoDuration;

    /**
     * 转写文本（仅当contentType为VIDEO且状态为COMPLETED时有值）
     */
    private String transcriptionText;

    /**
     * AI总结文本（当状态为COMPLETED时有值）
     */
    private String summaryText;

    /**
     * 详细结果（JSON对象）（当状态为COMPLETED时有值）
     * 包含完整的处理结果和元数据
     */
    private Object detailedResult;

    /**
     * 错误信息（仅当状态为FAILED时有值）
     */
    private String errorMessage;

    /**
     * 创建时间（ISO格式字符串）
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 完成时间（ISO格式字符串），仅当状态为COMPLETED或FAILED时有值
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    /**
     * 构造函数 - 用于创建任务响应
     * 
     * @param taskId 任务ID
     * @param status 任务状态
     * @param contentType 内容类型
     * @param message 状态消息
     */
    public LinkProcessResponseDto(String taskId, String status, String contentType, String message) {
        this.taskId = taskId;
        this.status = status;
        this.contentType = contentType;
        this.message = message;
    }

    /**
     * 构造函数 - 用于创建错误响应
     * 
     * @param taskId 任务ID
     * @param status 任务状态
     * @param errorMessage 错误信息
     */
    public LinkProcessResponseDto(String taskId, String status, String errorMessage) {
        this.taskId = taskId;
        this.status = status;
        this.errorMessage = errorMessage;
    }
}
