package com.ding.aiplatjava.dto;

import lombok.Data;

/**
 * OCR上传请求DTO
 * 用于接收前端上传文件时的参数
 */
@Data
public class OcrUploadRequestDto {

    /**
     * 是否使用PyPDF2处理
     * 默认为true
     */
    private boolean usePypdf2 = true;

    /**
     * 是否使用Docling处理
     * 默认为true
     */
    private boolean useDocling = true;

    /**
     * 是否使用Gemini处理
     * 默认为true
     */
    private boolean useGemini = true;

    /**
     * 是否使用Gemini Vision OCR
     * 默认为false
     */
    private boolean useVisionOcr = false;

    /**
     * 是否强制使用OCR
     * 默认为false
     */
    private boolean forceOcr = false;

    /**
     * 文档语言
     * 默认为auto（自动检测）
     */
    private String language = "auto";

    /**
     * Gemini模型选择
     * 可选值：gemini-2.5-pro, gemini-1.5-pro, gemini-1.5-flash
     * 默认为gemini-1.5-flash（最快速度）
     */
    private String geminiModel = "gemini-1.5-flash";
}
