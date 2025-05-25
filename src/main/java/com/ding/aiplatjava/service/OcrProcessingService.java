package com.ding.aiplatjava.service;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OCR处理服务接口
 * 定义OCR处理的核心功能
 */
public interface OcrProcessingService {

    /**
     * 处理文件并提取文本
     *
     * @param filePath 文件路径
     * @param options 处理选项
     * @return 处理结果，包含提取的文本和元数据
     */
    CompletableFuture<Map<String, Object>> processFile(Path filePath, Map<String, Object> options);

    /**
     * 从PDF文件中提取文本
     *
     * @param filePath PDF文件路径
     * @param forceOcr 是否强制使用OCR（即使PDF包含文本）
     * @return 提取的文本
     */
    CompletableFuture<String> extractTextFromPdf(Path filePath, boolean forceOcr);

    /**
     * 使用Docling进行OCR处理
     *
     * @param filePath 图像文件路径
     * @param language 语言代码（如"auto"、"zh"、"en"等）
     * @return OCR结果
     */
    CompletableFuture<String> processWithDocling(Path filePath, String language);

    /**
     * 使用Docling进行OCR处理（支持模型选择）
     *
     * @param filePath 图像文件路径
     * @param language 语言代码（如"auto"、"zh"、"en"等）
     * @param geminiModel Gemini模型选择
     * @return OCR结果
     */
    CompletableFuture<String> processWithDocling(Path filePath, String language, String geminiModel);

    /**
     * 使用Gemini Vision OCR处理扫描PDF和图像
     *
     * @param filePath 文件路径
     * @param language 语言代码
     * @param geminiModel Gemini模型选择
     * @return OCR结果，包含提取的文本和元数据
     */
    CompletableFuture<Map<String, Object>> processWithGeminiVisionOcr(Path filePath, String language, String geminiModel);

    /**
     * 使用Gemini进行文本分析和结构化
     *
     * @param text 要分析的文本
     * @param options 分析选项
     * @return 结构化的分析结果
     */
    CompletableFuture<Map<String, Object>> analyzeWithGemini(String text, Map<String, Object> options);
}
