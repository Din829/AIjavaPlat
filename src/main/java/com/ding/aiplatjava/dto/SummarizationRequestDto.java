package com.ding.aiplatjava.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL; // For URL validation
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 网页摘要请求的数据传输对象 (DTO)。
 * 包含需要进行摘要的网页 URL。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummarizationRequestDto {

    /**
     * 需要摘要的网页 URL。
     * 必须是有效的 URL 格式且不能为空。
     */
    @NotBlank(message = "URL不能为空")
    @URL(message = "必须是有效的URL格式")
    @Size(max = 2048, message = "URL长度不能超过2048个字符") // Add size constraint
    private String url;
} 