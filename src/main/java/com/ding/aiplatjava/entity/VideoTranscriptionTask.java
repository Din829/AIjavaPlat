package com.ding.aiplatjava.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 视频转写任务实体类
 * 对应数据库中的video_transcription_tasks表，用于存储用户的链接处理任务信息
 * 支持网页摘要和视频转写两种类型的处理
 */
@Data               // Lombok注解，自动生成getter、setter、equals、hashCode和toString方法
@NoArgsConstructor  // 自动生成无参构造函数
@AllArgsConstructor // 自动生成包含所有字段的构造函数
@Builder            // 自动生成建造者模式代码，方便对象创建
public class VideoTranscriptionTask {

    /**
     * 任务ID，自增主键
     */
    private Long id;

    /**
     * 用户ID，关联users表
     */
    private Long userId;

    /**
     * 唯一任务ID（UUID格式），用于前端查询任务状态
     */
    private String taskId;

    /**
     * 处理的URL链接
     * 可以是网页链接或视频链接
     */
    private String url;

    /**
     * 内容类型：WEBPAGE（网页）或 VIDEO（视频）
     */
    private String contentType;

    /**
     * 任务状态：PENDING（等待处理）, PROCESSING（处理中）, COMPLETED（已完成）, FAILED（失败）
     */
    private String status;

    /**
     * 视频标题
     * 仅当contentType为VIDEO时有值，用于增强转写精度
     */
    private String videoTitle;

    /**
     * 视频描述
     * 仅当contentType为VIDEO时有值，用于增强转写精度
     */
    private String videoDescription;

    /**
     * 视频时长（秒）
     * 仅当contentType为VIDEO时有值，支持小数点精度
     */
    private Double videoDuration;

    /**
     * 语言选择
     * 默认为'auto'（自动检测），可选值如'zh', 'en', 'ja'等
     */
    private String language;

    /**
     * 自定义prompt
     * 用户可以提供自定义的处理指令，用于AI分析和总结
     */
    private String customPrompt;

    /**
     * 完整的处理结果（JSON格式）
     * 存储详细的处理结果，包括原始数据和分析结果
     */
    private String resultJson;

    /**
     * 转写文本
     * 仅当contentType为VIDEO时有值，存储Whisper转写的完整文本
     */
    private String transcriptionText;

    /**
     * AI总结文本
     * 无论是网页还是视频，都会生成AI总结
     */
    private String summaryText;

    /**
     * 创建时间
     * 记录任务的创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * 记录任务信息的最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 完成时间
     * 记录任务的完成时间，仅当状态为COMPLETED或FAILED时有值
     */
    private LocalDateTime completedAt;

    /**
     * 错误信息
     * 当任务失败时，存储具体的错误信息
     */
    private String errorMessage;

    /**
     * 内容类型常量
     */
    public static class ContentType {
        public static final String WEBPAGE = "WEBPAGE";
        public static final String VIDEO = "VIDEO";
    }

    /**
     * 任务状态常量
     */
    public static class Status {
        public static final String PENDING = "PENDING";
        public static final String PROCESSING = "PROCESSING";
        public static final String COMPLETED = "COMPLETED";
        public static final String FAILED = "FAILED";
    }
}
