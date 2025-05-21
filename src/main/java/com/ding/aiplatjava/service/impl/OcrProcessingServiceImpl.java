package com.ding.aiplatjava.service.impl;

import com.ding.aiplatjava.service.OcrProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OCR处理服务实现类
 * 实现OCR处理的核心功能
 */
@Service
@RequiredArgsConstructor
public class OcrProcessingServiceImpl implements OcrProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OcrProcessingServiceImpl.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ocr.docling.url:http://localhost:8011/api/ocr}")
    private String doclingUrl;

    @Value("${ocr.gemini.api-key:AIzaSyDFLyEYqgaC6plSFF5IjvQEW0FEug6o14o}")
    private String geminiApiKey;

    @Value("${ocr.gemini.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro-preview-05-06:generateContent}")
    private String geminiUrl;

    /**
     * 处理文件并提取文本
     *
     * @param filePath 文件路径
     * @param options 处理选项
     * @return 处理结果，包含提取的文本和元数据
     */
    @Override
    @Async
    public CompletableFuture<Map<String, Object>> processFile(Path filePath, Map<String, Object> options) {
        log.info("开始处理文件: {}", filePath);

        try {
            // 获取选项参数
            boolean usePypdf2 = options.containsKey("usePypdf2") ? (boolean) options.get("usePypdf2") : true;
            boolean useDocling = options.containsKey("useDocling") ? (boolean) options.get("useDocling") : true;
            boolean useGemini = options.containsKey("useGemini") ? (boolean) options.get("useGemini") : true;
            boolean forceOcr = options.containsKey("forceOcr") ? (boolean) options.get("forceOcr") : false;
            String language = options.containsKey("language") ? (String) options.get("language") : "auto";

            // 结果Map
            Map<String, Object> result = new HashMap<>();

            // 文件类型检查
            String fileName = filePath.getFileName().toString().toLowerCase();
            String mimeType = Files.probeContentType(filePath);
            result.put("fileName", fileName);
            result.put("mimeType", mimeType);

            // 提取文本
            String extractedText;
            if (fileName.endsWith(".pdf")) {
                // PDF处理
                extractedText = extractTextFromPdf(filePath, forceOcr).get();

                // 如果PDF文本提取失败或为空，且启用了Docling，则使用Docling
                if ((extractedText == null || extractedText.trim().isEmpty()) && useDocling) {
                    log.info("PDF文本提取为空，尝试使用Docling进行OCR");
                    extractedText = processWithDocling(filePath, language).get();
                }
            } else if (isImageFile(fileName) && useDocling) {
                // 图像处理
                extractedText = processWithDocling(filePath, language).get();
            } else {
                // 不支持的文件类型
                throw new UnsupportedOperationException("不支持的文件类型: " + mimeType);
            }

            // 检查提取的文本是否为空
            if (extractedText == null || extractedText.trim().isEmpty()) {
                log.warn("提取的文本为空: {}", filePath);
                result.put("extractedText", "");
                result.put("warning", "未能提取到文本内容");
            } else {
                log.info("成功提取文本，长度: {}", extractedText.length());
                result.put("extractedText", extractedText);

                // 添加文本摘要（前100个字符）用于调试
                String textSummary = extractedText.length() > 100
                    ? extractedText.substring(0, 100) + "..."
                    : extractedText;
                result.put("textSummary", textSummary);
            }

            // 使用Gemini进行文本分析
            if (useGemini && extractedText != null && !extractedText.trim().isEmpty()) {
                log.info("开始使用Gemini分析文本");
                Map<String, Object> geminiOptions = new HashMap<>();
                geminiOptions.put("language", language);
                Map<String, Object> analysisResult = analyzeWithGemini(extractedText, geminiOptions).get();

                if (analysisResult.containsKey("error")) {
                    log.warn("Gemini分析失败: {}", analysisResult.get("error"));
                } else {
                    log.info("Gemini分析成功");
                }

                result.put("analysis", analysisResult);
            } else if (useGemini) {
                log.warn("跳过Gemini分析，因为提取的文本为空");
                result.put("analysis", Map.of("warning", "无法分析空文本"));
            }

            log.info("文件处理完成: {}", filePath);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("处理文件时发生错误: {}", filePath, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * 从PDF文件中提取文本
     *
     * @param filePath PDF文件路径
     * @param forceOcr 是否强制使用OCR（即使PDF包含文本）
     * @return 提取的文本
     */
    @Override
    @Async
    public CompletableFuture<String> extractTextFromPdf(Path filePath, boolean forceOcr) {
        log.info("从PDF提取文本: {}, forceOcr: {}", filePath, forceOcr);

        if (forceOcr) {
            log.info("强制使用OCR，跳过PDF文本提取");
            return CompletableFuture.completedFuture("");
        }

        try (PDDocument document = PDDocument.load(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // 如果提取的文本太少，可能是扫描PDF
            if (text.trim().length() < 100) {
                log.info("PDF文本内容太少，可能是扫描PDF: {}", filePath);
                return CompletableFuture.completedFuture("");
            }

            log.info("PDF文本提取成功: {}", filePath);
            return CompletableFuture.completedFuture(text);
        } catch (IOException e) {
            log.error("PDF文本提取失败: {}", filePath, e);
            return CompletableFuture.completedFuture("");
        }
    }

    /**
     * 使用Docling进行OCR处理
     *
     * @param filePath 图像文件路径
     * @param language 语言代码（如"auto"、"zh"、"en"等）
     * @return OCR结果
     */
    @Override
    @Async
    public CompletableFuture<String> processWithDocling(Path filePath, String language) {
        log.info("使用Docling处理文件: {}, 语言: {}", filePath, language);

        try {
            // 读取文件
            byte[] fileBytes = Files.readAllBytes(filePath);

            // 准备请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return filePath.getFileName().toString();
                }
            });
            body.add("language", language);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 调用Docling OCR服务
            ResponseEntity<String> response = restTemplate.postForEntity(
                doclingUrl,
                requestEntity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 解析JSON响应
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String extractedText = jsonNode.path("text").asText("");

                log.info("Docling OCR处理成功: {}", filePath);
                return CompletableFuture.completedFuture(extractedText);
            } else {
                log.error("Docling OCR处理失败: {}, 状态码: {}", filePath, response.getStatusCode());
                return CompletableFuture.completedFuture("");
            }
        } catch (Exception e) {
            log.error("Docling OCR处理异常: {}", filePath, e);
            return CompletableFuture.completedFuture("");
        }
    }

    /**
     * 使用Gemini进行文本分析和结构化
     *
     * @param text 要分析的文本
     * @param options 分析选项
     * @return 结构化的分析结果
     */
    @Override
    @Async
    public CompletableFuture<Map<String, Object>> analyzeWithGemini(String text, Map<String, Object> options) {
        log.info("使用Gemini分析文本，文本长度: {}", text.length());

        try {
            // 准备请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            // 构建Gemini请求体
            Map<String, Object> requestBody = new HashMap<>();

            // 构建提示词
            String prompt = "请分析以下文本，提取关键信息并以JSON格式返回结构化数据。\n\n" + text;

            // 构建contents数组
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            content.put("role", "user");

            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            parts.add(part);

            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);

            // 添加生成参数
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.2);
            generationConfig.put("topP", 0.8);
            generationConfig.put("topK", 40);
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // 调用Gemini API
            ResponseEntity<String> response = restTemplate.postForEntity(
                geminiUrl,
                requestEntity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 解析JSON响应
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String responseText = "";

                // 检查响应格式
                if (jsonNode.has("candidates") &&
                    jsonNode.path("candidates").isArray() &&
                    jsonNode.path("candidates").size() > 0) {

                    responseText = jsonNode.path("candidates")
                                          .path(0)
                                          .path("content")
                                          .path("parts")
                                          .path(0)
                                          .path("text")
                                          .asText("");
                } else {
                    log.warn("Gemini API响应格式不符合预期: {}", response.getBody());
                    return CompletableFuture.completedFuture(Map.of("error", "Gemini API响应格式不符合预期"));
                }

                // 尝试从响应文本中提取JSON
                Map<String, Object> analysisResult = extractJsonFromText(responseText);

                log.info("Gemini分析成功，提取到结构化数据");
                return CompletableFuture.completedFuture(analysisResult);
            } else {
                log.error("Gemini分析失败，状态码: {}", response.getStatusCode());
                return CompletableFuture.completedFuture(Map.of("error", "Gemini API调用失败"));
            }
        } catch (Exception e) {
            log.error("Gemini分析异常", e);
            return CompletableFuture.completedFuture(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 从文本中提取JSON
     *
     * @param text 包含JSON的文本
     * @return 提取的JSON对象
     */
    private Map<String, Object> extractJsonFromText(String text) {
        try {
            // 查找JSON开始和结束的位置
            int startIndex = text.indexOf('{');
            int endIndex = text.lastIndexOf('}') + 1;

            if (startIndex >= 0 && endIndex > startIndex) {
                String jsonStr = text.substring(startIndex, endIndex);
                return objectMapper.readValue(jsonStr, Map.class);
            } else {
                // 如果没有找到JSON，返回原始文本
                return Map.of("text", text);
            }
        } catch (Exception e) {
            log.error("从文本中提取JSON失败", e);
            return Map.of("text", text);
        }
    }

    /**
     * 判断文件是否为图像文件
     *
     * @param fileName 文件名
     * @return 是否为图像文件
     */
    private boolean isImageFile(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".jpg") ||
               lowerCaseFileName.endsWith(".jpeg") ||
               lowerCaseFileName.endsWith(".png") ||
               lowerCaseFileName.endsWith(".gif") ||
               lowerCaseFileName.endsWith(".bmp") ||
               lowerCaseFileName.endsWith(".tiff") ||
               lowerCaseFileName.endsWith(".tif");
    }
}
