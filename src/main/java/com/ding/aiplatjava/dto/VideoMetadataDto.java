package com.ding.aiplatjava.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 视频元数据的数据传输对象 (DTO)
 * 用于在Java服务和视频处理微服务之间传输视频信息
 * 
 * @author Ding
 * @since 2024-12-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoMetadataDto {

    /**
     * 视频ID
     * 由视频平台提供的唯一标识符
     */
    private String videoId;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频描述
     */
    private String description;

    /**
     * 视频时长（秒）
     * 支持小数点精度，如147.4秒
     */
    private Double duration;

    /**
     * WAV音频文件下载URL
     * 由视频处理微服务生成的音频文件访问地址
     */
    private String wavDownloadUrl;

    /**
     * 视频平台
     * 例如：YouTube, Bilibili, Vimeo等
     */
    private String platform;

    /**
     * 处理是否成功
     */
    private Boolean success;

    /**
     * 错误信息（如果处理失败）
     */
    private String errorMessage;
}
