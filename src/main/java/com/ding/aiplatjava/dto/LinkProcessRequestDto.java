package com.ding.aiplatjava.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 链接处理请求的数据传输对象 (DTO)
 * 用于接收用户提交的链接处理请求，支持网页摘要和视频转写
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkProcessRequestDto {

    /**
     * 需要处理的URL链接
     * 可以是网页链接或视频链接，系统会自动识别类型
     */
    @NotBlank(message = "URL不能为空")
    @URL(message = "必须是有效的URL格式")
    @Size(max = 2048, message = "URL长度不能超过2048个字符")
    private String url;

    /**
     * 语言选择
     * 默认为'auto'（自动检测）
     * 可选值：'auto', 'zh', 'en', 'ja'等
     */
    @Size(max = 10, message = "语言代码长度不能超过10个字符")
    private String language = "auto";

    /**
     * 自定义prompt
     * 用户可以提供自定义的处理指令，用于AI分析和总结
     */
    @Size(max = 2000, message = "自定义prompt长度不能超过2000个字符")
    private String customPrompt;
}
