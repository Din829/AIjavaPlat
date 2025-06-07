package com.ding.aiplatjava.service.impl;

import com.ding.aiplatjava.dto.VideoMetadataDto;
import com.ding.aiplatjava.dto.TranscriptionResultDto;
import com.ding.aiplatjava.service.VideoProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频处理服务实现类
 * 负责与视频处理微服务和Whisper微服务的通信
 * 
 * @author Ding
 * @since 2024-12-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoProcessingServiceImpl implements VideoProcessingService {

    @Qualifier("videoProcessingRestTemplate")
    private final RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper;

    @Value("${video.processing.service.url}")
    private String videoServiceUrl;

    @Value("${whisper.service.url}")
    private String whisperServiceUrl;

    /**
     * 获取视频处理服务的基础URL
     *
     * @return 视频处理服务URL
     */
    @Override
    public String getVideoServiceUrl() {
        return videoServiceUrl;
    }

    @Override
    public VideoMetadataDto processVideo(String url, String language) {
        log.info("开始处理视频: {}, 语言: {}", url, language);
        
        try {
            // 1. 准备请求数据
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("url", url);
            requestData.put("language", language != null ? language : "auto");

            // 2. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestData, headers);

            // 3. 调用视频处理微服务
            String processVideoUrl = videoServiceUrl + "/process-video";
            log.debug("调用视频处理服务: {}", processVideoUrl);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                processVideoUrl,
                requestEntity,
                String.class
            );

            // 4. 解析响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                
                // 检查处理是否成功
                boolean success = jsonNode.path("success").asBoolean(false);
                if (!success) {
                    String errorMessage = jsonNode.path("error_message").asText("视频处理失败");
                    throw new RuntimeException("视频处理微服务返回错误: " + errorMessage);
                }

                // 5. 构建返回结果
                return VideoMetadataDto.builder()
                        .videoId(jsonNode.path("video_id").asText(""))
                        .title(jsonNode.path("title").asText(""))
                        .description(jsonNode.path("description").asText(""))
                        .duration(jsonNode.path("duration").asDouble(0.0))
                        .wavDownloadUrl(jsonNode.path("wav_download_url").asText(""))
                        .platform(jsonNode.path("platform").asText(""))
                        .success(true)
                        .build();
            } else {
                throw new RuntimeException("视频处理微服务调用失败，状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("视频处理失败: {}", url, e);
            return VideoMetadataDto.builder()
                    .success(false)
                    .errorMessage("视频处理失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public TranscriptionResultDto transcribeAudio(String wavUrl, VideoMetadataDto metadata, String customPrompt) {
        log.info("开始转写音频: {}", wavUrl);
        
        try {
            // 1. 下载WAV文件
            byte[] audioData = downloadWavFile(wavUrl);
            if (audioData == null || audioData.length == 0) {
                throw new RuntimeException("无法下载音频文件: " + wavUrl);
            }

            // 2. 准备multipart请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // 添加音频文件
            body.add("file", new ByteArrayResource(audioData) {
                @Override
                public String getFilename() {
                    return metadata.getVideoId() + ".wav";
                }
            });

            // 添加其他参数 - 不传language参数让Whisper自动检测语言
            // body.add("language", null); // Whisper会自动检测语言
            if (metadata.getTitle() != null && !metadata.getTitle().isEmpty()) {
                body.add("title", metadata.getTitle());
            }
            if (metadata.getDescription() != null && !metadata.getDescription().isEmpty()) {
                body.add("description", metadata.getDescription());
            }
            if (customPrompt != null && !customPrompt.isEmpty()) {
                body.add("custom_prompt", customPrompt);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 3. 调用Whisper转写微服务
            String transcribeUrl = whisperServiceUrl + "/transcribe";
            log.debug("调用Whisper转写服务: {}", transcribeUrl);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                transcribeUrl,
                requestEntity,
                String.class
            );

            // 4. 解析响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                
                // 解析分段信息
                List<TranscriptionResultDto.TranscriptionSegment> segments = new ArrayList<>();
                JsonNode segmentsNode = jsonNode.path("segments");
                if (segmentsNode.isArray()) {
                    for (JsonNode segmentNode : segmentsNode) {
                        TranscriptionResultDto.TranscriptionSegment segment = 
                            TranscriptionResultDto.TranscriptionSegment.builder()
                                .start(segmentNode.path("start").asDouble(0.0))
                                .end(segmentNode.path("end").asDouble(0.0))
                                .text(segmentNode.path("text").asText(""))
                                .build();
                        segments.add(segment);
                    }
                }

                // 解析处理信息
                Map<String, Object> processingInfo = new HashMap<>();
                JsonNode processingInfoNode = jsonNode.path("processing_info");
                if (processingInfoNode.isObject()) {
                    processingInfoNode.fields().forEachRemaining(entry -> {
                        processingInfo.put(entry.getKey(), entry.getValue().asText());
                    });
                }

                // 5. 构建返回结果
                return TranscriptionResultDto.builder()
                        .language(jsonNode.path("language").asText(""))
                        .languageProbability(jsonNode.path("language_probability").asDouble(0.0))
                        .segments(segments)
                        .fullText(jsonNode.path("full_text").asText(""))
                        .processingInfo(processingInfo)
                        .build();
            } else {
                throw new RuntimeException("Whisper转写微服务调用失败，状态码: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("音频转写失败: {}", wavUrl, e);
            throw new RuntimeException("音频转写失败: " + e.getMessage(), e);
        }
    }

    @Override
    public TranscriptionResultDto processVideoComplete(String url, String language, String customPrompt) {
        log.info("开始完整视频处理流程: {}", url);
        
        try {
            // 1. 处理视频，获取元数据
            VideoMetadataDto metadata = processVideo(url, language);
            if (!metadata.getSuccess()) {
                throw new RuntimeException("视频处理失败: " + metadata.getErrorMessage());
            }

            // 2. 转写音频
            String fullWavUrl = videoServiceUrl + metadata.getWavDownloadUrl();
            return transcribeAudio(fullWavUrl, metadata, customPrompt);

        } catch (Exception e) {
            log.error("完整视频处理流程失败: {}", url, e);
            throw new RuntimeException("完整视频处理失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isVideoServiceHealthy() {
        try {
            String healthUrl = videoServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("视频处理服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isWhisperServiceHealthy() {
        try {
            String healthUrl = whisperServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Whisper转写服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 下载WAV音频文件
     * 
     * @param wavUrl WAV文件URL
     * @return 音频文件字节数组
     */
    private byte[] downloadWavFile(String wavUrl) {
        try {
            log.debug("下载音频文件: {}", wavUrl);
            ResponseEntity<byte[]> response = restTemplate.getForEntity(wavUrl, byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("音频文件下载成功，大小: {} bytes", response.getBody().length);
                return response.getBody();
            } else {
                log.error("音频文件下载失败，状态码: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("下载音频文件异常: {}", wavUrl, e);
            return null;
        }
    }
}
