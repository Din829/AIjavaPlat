package com.ding.aiplatjava.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Prompt 数据传输对象 (DTO)。
 * 用于 API 响应，选择性暴露字段并格式化数据。
 */
@Data
public class PromptDto {
    private Long id;
    // private Long userId; // 列表通常不返回 userId
    private String title;
    private String content;
    private String category;

    /**
     * 创建时间，格式化为 ISO 8601 风格。
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间，格式化为 ISO 8601 风格。
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
} 