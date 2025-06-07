package com.ding.aiplatjava.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * 转写结果的数据传输对象 (DTO)
 * 用于在Java服务和Whisper微服务之间传输转写结果
 * 
 * @author Ding
 * @since 2024-12-28
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptionResultDto {

    /**
     * 检测到的语言
     */
    private String language;

    /**
     * 语言检测置信度
     */
    @JsonProperty("language_probability")
    private Double languageProbability;

    /**
     * 转写分段信息
     * 每个分段包含开始时间、结束时间和文本内容
     */
    private List<TranscriptionSegment> segments;

    /**
     * 完整的转写文本
     */
    @JsonProperty("full_text")
    private String fullText;

    /**
     * 处理信息
     * 包含分段数量、文本长度、使用的模型等元数据
     */
    @JsonProperty("processing_info")
    private Map<String, Object> processingInfo;

    /**
     * 转写分段内部类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TranscriptionSegment {
        
        /**
         * 分段开始时间（秒）
         */
        private Double start;

        /**
         * 分段结束时间（秒）
         */
        private Double end;

        /**
         * 分段文本内容
         */
        private String text;
    }

    /**
     * 将转写结果转换为JSON字符串
     * 用于存储到数据库
     * 
     * @return JSON格式的转写结果
     */
    public String toJson() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * 获取转写分段数量
     * 
     * @return 分段数量
     */
    public int getSegmentCount() {
        return segments != null ? segments.size() : 0;
    }

    /**
     * 获取转写文本长度
     * 
     * @return 文本长度
     */
    public int getTextLength() {
        return fullText != null ? fullText.length() : 0;
    }

    /**
     * 获取视频总时长（基于最后一个分段的结束时间）
     * 
     * @return 视频时长（秒）
     */
    public Double getTotalDuration() {
        if (segments == null || segments.isEmpty()) {
            return 0.0;
        }
        return segments.get(segments.size() - 1).getEnd();
    }
}
