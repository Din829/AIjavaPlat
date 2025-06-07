package com.ding.aiplatjava.service;

import com.ding.aiplatjava.dto.VideoMetadataDto;
import com.ding.aiplatjava.dto.TranscriptionResultDto;

/**
 * 视频处理服务接口
 * 提供视频下载、元数据提取、音频转换和转写功能
 * 
 * @author Ding
 * @since 2024-12-28
 */
public interface VideoProcessingService {

    /**
     * 处理视频链接，提取元数据并转换为WAV格式
     * 
     * @param url 视频URL
     * @param language 语言代码（可选）
     * @return 视频元数据，包含WAV下载URL
     * @throws RuntimeException 如果视频处理失败
     */
    VideoMetadataDto processVideo(String url, String language);

    /**
     * 转写音频文件
     * 
     * @param wavUrl WAV音频文件URL
     * @param metadata 视频元数据（用于增强转写精度）
     * @param customPrompt 自定义prompt（可选）
     * @return 转写结果，包含分段信息和完整文本
     * @throws RuntimeException 如果转写失败
     */
    TranscriptionResultDto transcribeAudio(String wavUrl, VideoMetadataDto metadata, String customPrompt);

    /**
     * 完整的视频处理流程：下载 -> 转换 -> 转写
     * 
     * @param url 视频URL
     * @param language 语言代码（可选）
     * @param customPrompt 自定义prompt（可选）
     * @return 转写结果
     * @throws RuntimeException 如果处理失败
     */
    TranscriptionResultDto processVideoComplete(String url, String language, String customPrompt);

    /**
     * 检查视频处理微服务的健康状态
     * 
     * @return true如果服务正常，false如果服务不可用
     */
    boolean isVideoServiceHealthy();

    /**
     * 检查Whisper转写微服务的健康状态
     * 
     * @return true如果服务正常，false如果服务不可用
     */
    boolean isWhisperServiceHealthy();

    /**
     * 获取视频处理微服务的URL
     * 
     * @return 视频处理微服务的URL字符串
     */
    String getVideoServiceUrl();
}
