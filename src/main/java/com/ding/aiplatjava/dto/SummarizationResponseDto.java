package com.ding.aiplatjava.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 网页摘要响应的数据传输对象 (DTO)。
 * 包含由 AI 服务生成的摘要文本。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SummarizationResponseDto {

    /**
     * 生成的摘要文本。
     */
    private String summary;

    /**
     * 手动添加构造函数，以防Lombok注解不起作用。
     *
     * @param summary 摘要文本
     */
    public SummarizationResponseDto(String summary) {
        this.summary = summary;
    }
}