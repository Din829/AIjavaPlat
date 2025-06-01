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

// Apache POI imports for Excel processing
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.FileInputStream;

// Apache POI imports for Word processing
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;

// Additional imports for text file processing
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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

    @Value("${ocr.docling.url:http://localhost:8012/api/ocr/upload}")
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
            boolean useVisionOcr = options.containsKey("useVisionOcr") ? (boolean) options.get("useVisionOcr") : false;
            boolean forceOcr = options.containsKey("forceOcr") ? (boolean) options.get("forceOcr") : false;
            String language = options.containsKey("language") ? (String) options.get("language") : "auto";
            String geminiModel = options.containsKey("geminiModel") ? (String) options.get("geminiModel") : "gemini-1.5-flash";

            // 添加调试日志
            log.info("处理选项: usePypdf2={}, useDocling={}, useGemini={}, useVisionOcr={}, forceOcr={}, language={}, geminiModel={}",
                usePypdf2, useDocling, useGemini, useVisionOcr, forceOcr, language, geminiModel);

            // 结果Map
            Map<String, Object> result = new HashMap<>();

            // 文件类型检查
            String fileName = filePath.getFileName().toString().toLowerCase();
            String mimeType = Files.probeContentType(filePath);
            result.put("fileName", fileName);
            result.put("mimeType", mimeType);

            // ========== 关键修复：当启用Vision OCR时，完全跳过所有其他处理逻辑 ==========
            if (useVisionOcr) {
                log.info("启用了Vision OCR，直接使用Gemini Vision OCR处理文件: {}", filePath);
                log.info("Vision OCR模式：跳过所有常规处理流程，包括PyPDF2、Docling和常规Gemini分析");

                // 使用用户选择的模型，如果没有选择则使用最佳OCR模型
                String visionModel = geminiModel;
                if (visionModel == null || visionModel.isEmpty()) {
                    visionModel = "gemini-2.5-pro-preview-05-06";
                }
                log.info("使用模型进行Vision OCR: {}", visionModel);

                try {
                    log.info("开始调用processWithGeminiVisionOcr方法");
                    Map<String, Object> visionOcrResult = processWithGeminiVisionOcr(filePath, language, visionModel).get();
                    log.info("processWithGeminiVisionOcr方法调用完成，结果: {}", visionOcrResult.keySet());

                    if (visionOcrResult.containsKey("error")) {
                        log.warn("Gemini Vision OCR失败: {}", visionOcrResult.get("error"));
                        result.put("extractedText", "");
                        result.put("warning", "Vision OCR处理失败: " + visionOcrResult.get("error"));
                        result.put("analysis", Map.of("error", visionOcrResult.get("error")));
                    } else {
                        log.info("Gemini Vision OCR成功");
                        String visionText = (String) visionOcrResult.get("extractedText");
                        result.put("extractedText", visionText != null ? visionText : "");

                        // 修正 analysis 字段的赋值
                        // visionOcrResult 本身是从 processWithGeminiVisionOcr 返回的 Map，
                        // 它内部的 "analysis" 键对应的值已经是 "" (空字符串) 或实际的gemini分析内容（理论上Vision OCR后无分析则为空）
                        // 我们应该直接使用这个内部的 analysis 值，而不是把整个 visionOcrResult 赋给顶层 result 的 analysis。
                        // result.put("analysis", visionOcrResult.getOrDefault("analysis", "")); // 旧逻辑，仅赋值空字符串

                        // 处理 warning
                        if (visionText != null && !visionText.trim().isEmpty()) {
                            if (visionOcrResult.containsKey("warning") && visionOcrResult.get("warning") != null && !((String)visionOcrResult.get("warning")).isEmpty()) {
                                result.put("warning", visionOcrResult.get("warning"));
                            } else {
                                result.put("warning", "使用Gemini Vision OCR提取的文本");
                            }
                        } else {
                            result.put("warning", "Vision OCR未能提取到文本");
                        }

                        // 新增逻辑：如果同时useGemini为true，则进行后续的文本分析
                        if (useGemini && visionText != null && !visionText.trim().isEmpty()) {
                            log.info("Vision OCR提取文本后，继续使用Gemini进行内容分析");
                            Map<String, Object> geminiOptions = new HashMap<>();
                            geminiOptions.put("language", language); // 使用顶层传入的language
                            geminiOptions.put("geminiModel", geminiModel); // 使用顶层传入的geminiModel进行分析

                            try {
                                Map<String, Object> analysisResultFromGemini = analyzeWithGemini(visionText, geminiOptions).get();
                                if (analysisResultFromGemini.containsKey("error")) {
                                    log.warn("对Vision OCR文本的Gemini分析失败: {}", analysisResultFromGemini.get("error"));
                                    result.put("analysis", Map.of("warning", "对Vision OCR文本的Gemini分析失败: " + analysisResultFromGemini.get("error")));
                                } else {
                                    log.info("对Vision OCR文本的Gemini分析成功");
                                    result.put("analysis", analysisResultFromGemini);
                                }
                            } catch (Exception e_gemini_analysis) {
                                log.error("对Vision OCR文本进行Gemini分析时发生异常", e_gemini_analysis);
                                result.put("analysis", Map.of("error", "对Vision OCR文本进行Gemini分析时发生异常: " + e_gemini_analysis.getMessage()));
                            }
                        } else if (useGemini && (visionText == null || visionText.trim().isEmpty())) { // useGemini is true, but no visionText
                             log.warn("希望进行Gemini分析，但Vision OCR未能提取到文本。");
                             result.put("analysis", Map.of("warning", "Vision OCR未能提取文本，无法进行Gemini分析"));
                        } else {
                            // useGemini is false, analysis 应该来自 visionOcrResult (通常是空字符串 "")
                            result.put("analysis", visionOcrResult.getOrDefault("analysis", ""));
                        }
                    }

                    // log.info("Vision OCR处理完成: {}", filePath);
                    // log.info("Vision OCR流程结束，直接返回结果，跳过所有常规处理");
                    // return CompletableFuture.completedFuture(result); // <--- 移除此处的直接返回

                } catch (Exception e) {
                    log.error("Vision OCR处理过程中发生异常: {}", filePath, e);
                    result.put("extractedText", "");
                    result.put("warning", "Vision OCR处理异常: " + e.getMessage());
                    result.put("analysis", Map.of("error", "Vision OCR处理异常: " + e.getMessage()));
                    return CompletableFuture.completedFuture(result); // Vision OCR 本身失败，则直接返回
                }
                // 如果Vision OCR流程执行完毕（无论是否后续调用了Gemini分析），都从这里返回
                log.info("Vision OCR流程（可能包括后续Gemini分析）执行完毕，准备返回结果");
                return CompletableFuture.completedFuture(result);
            }

            // ========== 以下是常规处理流程，只有在 useVisionOcr=false 时才执行 ==========
            log.info("开始常规处理流程（非Vision OCR模式）");
            
            // 常规处理流程
            String extractedText;
            Map<String, Object> pythonResult = null;

            if (fileName.endsWith(".pdf")) {
                // PDF处理 - 总是调用Python服务以确保图像提取
                log.info("PDF文件检测到，调用Python服务进行完整处理（包括图像提取）");

                // 调用Python服务进行完整的PDF处理
                pythonResult = callPythonServiceForPdf(filePath, usePypdf2, useDocling, useGemini, forceOcr, language, geminiModel);

                if (pythonResult != null && pythonResult.containsKey("full_text")) {
                    extractedText = (String) pythonResult.get("full_text");
                    log.info("从Python服务获取文本，长度: {}", extractedText != null ? extractedText.length() : 0);
                } else {
                    // 如果Python服务失败，回退到Java本地处理
                    log.warn("Python服务处理失败，回退到Java本地PDF处理");
                    extractedText = extractTextFromPdf(filePath, forceOcr).get();

                    // 如果PDF文本提取失败或为空，且启用了Docling，则使用Docling
                    log.debug("检查是否需要使用Docling: extractedText={}, useDocling={}",
                        (extractedText == null ? "null" : "length=" + extractedText.length()), useDocling);
                    if ((extractedText == null || extractedText.trim().isEmpty()) && useDocling) {
                        log.info("PDF文本提取为空，尝试使用Docling进行OCR");
                        extractedText = processWithDocling(filePath, language, geminiModel).get();
                    }
                }
            } else if (isExcelFile(fileName)) {
                // Excel文件处理
                log.info("检测到Excel文件，使用Apache POI进行文本提取");
                extractedText = extractTextFromExcel(filePath, language, geminiModel).get();
            } else if (isWordFile(fileName)) {
                // Word文档处理
                log.info("检测到Word文档，使用Apache POI进行文本提取");
                extractedText = extractTextFromWord(filePath, language, geminiModel).get();
            } else if (isTextFile(fileName)) {
                // 纯文本文件处理
                log.info("检测到文本文件，直接读取文本内容");
                extractedText = extractTextFromTextFile(filePath).get();
            } else if (isCsvFile(fileName)) {
                // CSV文件处理
                log.info("检测到CSV文件，解析表格数据");
                extractedText = extractTextFromCsv(filePath).get();
            } else if (isImageFile(fileName) && useDocling) {
                // 图像处理
                extractedText = processWithDocling(filePath, language, geminiModel).get();
            } else {
                // 不支持的文件类型
                throw new UnsupportedOperationException("不支持的文件类型: " + mimeType);
            }

            // 检查提取的文本是否为空
            boolean hasText = extractedText != null && !extractedText.trim().isEmpty();

            if (hasText) {
                log.info("成功提取文本，长度: {}", extractedText.length());
                result.put("extractedText", extractedText);

                // 添加文本摘要（前100个字符）用于调试
                String textSummary = extractedText.length() > 100
                    ? extractedText.substring(0, 100) + "..."
                    : extractedText;
                result.put("textSummary", textSummary);
            } else {
                log.warn("常规方法未能提取到文本: {}", filePath);
                result.put("extractedText", "");
                result.put("warning", "常规方法未能提取到文本内容");
            }

            // 如果从Python服务获取了图像信息，添加到结果中
            if (pythonResult != null && pythonResult.containsKey("images")) {
                List<?> images = (List<?>) pythonResult.get("images");
                result.put("images", images);
                log.info("添加图像信息到结果中，图像数量: {}", images.size());
            } else {
                result.put("images", new ArrayList<>());
            }

            // 使用Gemini进行处理（仅用于文本分析，不再进行OCR）
            if (useGemini && hasText) {
                // 只有当有文本时才进行分析
                log.info("开始使用Gemini分析文本");
                Map<String, Object> geminiOptions = new HashMap<>();
                geminiOptions.put("language", language);
                geminiOptions.put("geminiModel", geminiModel);
                Map<String, Object> analysisResult = analyzeWithGemini(extractedText, geminiOptions).get();

                if (analysisResult.containsKey("error")) {
                    log.warn("Gemini分析失败: {}", analysisResult.get("error"));
                } else {
                    log.info("Gemini分析成功");
                }

                result.put("analysis", analysisResult);
            } else if (useGemini && !hasText) {
                // 当没有文本时，不再自动尝试Vision OCR，避免重复调用
                log.info("常规方法未能提取到文本，且未启用Vision OCR，跳过进一步处理");
                result.put("analysis", Map.of("warning", "无法分析空文本，建议启用Vision OCR处理扫描文档"));
            }

            log.info("文件处理完成: {}", filePath);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("处理文件时发生错误: {}", filePath, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("extractedText", "");
            errorResult.put("warning", "处理过程中发生错误: " + e.getMessage());
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
     * 调用Python服务进行PDF完整处理（包括图像提取）
     *
     * @param filePath PDF文件路径
     * @param usePypdf2 是否使用PyPDF2
     * @param useDocling 是否使用Docling
     * @param useGemini 是否使用Gemini
     * @param forceOcr 是否强制OCR
     * @param language 语言代码
     * @param geminiModel Gemini模型选择
     * @return 完整的处理结果，包含文本和图像信息
     */
    private Map<String, Object> callPythonServiceForPdf(Path filePath, boolean usePypdf2, boolean useDocling,
                                                       boolean useGemini, boolean forceOcr, String language, String geminiModel) {
        log.info("调用Python服务处理PDF: {}, 参数: pypdf2={}, docling={}, gemini={}, forceOcr={}, language={}, model={}",
                filePath, usePypdf2, useDocling, useGemini, forceOcr, language, geminiModel);

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

            // 传递用户选择的参数（字符串格式）
            body.add("use_pypdf2", String.valueOf(usePypdf2));
            body.add("use_docling", String.valueOf(useDocling));
            body.add("use_gemini", String.valueOf(useGemini));
            body.add("force_ocr", String.valueOf(forceOcr));
            body.add("language", language);
            body.add("gemini_model", geminiModel);
            body.add("use_vision_ocr", "false");  // 常规处理模式

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 调用Python OCR服务
            ResponseEntity<String> response = restTemplate.postForEntity(
                doclingUrl,
                requestEntity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 解析JSON响应
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                Map<String, Object> result = new HashMap<>();

                // 提取文本
                String fullText = jsonNode.path("full_text").asText("");
                result.put("full_text", fullText);

                // 提取图像信息
                JsonNode imagesNode = jsonNode.path("images");
                if (imagesNode.isArray() && imagesNode.size() > 0) {
                    List<Map<String, Object>> images = new ArrayList<>();
                    for (JsonNode imageNode : imagesNode) {
                        Map<String, Object> imageInfo = new HashMap<>();
                        imageInfo.put("image_id", imageNode.path("image_id").asText(""));
                        imageInfo.put("page_number", imageNode.path("page_number").asInt(1));
                        imageInfo.put("description", imageNode.path("description").asText(""));
                        imageInfo.put("mime_type", imageNode.path("mime_type").asText(""));
                        imageInfo.put("data", imageNode.path("data").asText(""));
                        images.add(imageInfo);
                    }
                    result.put("images", images);
                    log.info("从Python服务获取到{}个图像", images.size());
                } else {
                    result.put("images", new ArrayList<>());
                }

                // 提取其他信息
                result.put("processing_info", jsonNode.path("processing_info"));
                result.put("document_metadata", jsonNode.path("document_metadata"));

                log.info("Python服务PDF处理成功: {}, 文本长度: {}, 图像数量: {}",
                        filePath, fullText.length(), ((List<?>) result.get("images")).size());
                return result;
            } else {
                log.error("Python服务PDF处理失败: {}, 状态码: {}", filePath, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("调用Python服务处理PDF异常: {}", filePath, e);
            return null;
        }
    }

    /**
     * 使用Docling进行OCR处理（原版本，向后兼容）
     *
     * @param filePath 图像文件路径
     * @param language 语言代码（如"auto"、"zh"、"en"等）
     * @return OCR结果
     */
    @Override
    @Async
    public CompletableFuture<String> processWithDocling(Path filePath, String language) {
        // 调用带模型参数的版本，使用默认模型
        return processWithDocling(filePath, language, "gemini-1.5-flash");
    }

    /**
     * 使用Docling进行OCR处理
     *
     * @param filePath 图像文件路径
     * @param language 语言代码（如"auto"、"zh"、"en"等）
     * @param geminiModel Gemini模型选择（可选）
     * @return OCR结果
     */
    @Override
    @Async
    public CompletableFuture<String> processWithDocling(Path filePath, String language, String geminiModel) {
        log.info("使用Docling处理文件: {}, 语言: {}, 模型: {}", filePath, language, geminiModel);

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
            // 传递默认参数以确保兼容性（字符串格式）
            body.add("use_pypdf2", "true");       // 默认启用PyPDF2
            body.add("use_docling", "true");      // 默认启用Docling
            body.add("use_gemini", "true");       // 默认启用Gemini分析
            body.add("force_ocr", "false");       // 默认不强制OCR
            body.add("language", language);
            body.add("gemini_model", geminiModel);  // 添加模型选择参数
            body.add("use_vision_ocr", "false");  // 默认不启用Vision OCR

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
     * 使用Gemini Vision OCR处理扫描PDF
     *
     * @param filePath PDF文件路径
     * @param language 语言代码
     * @param geminiModel Gemini模型选择
     * @return OCR结果
     */
    @Override
    @Async
    public CompletableFuture<Map<String, Object>> processWithGeminiVisionOcr(Path filePath, String language, String geminiModel) {
        log.info("使用Gemini Vision OCR处理文件: {}, 语言: {}, 模型: {}", filePath, language, geminiModel);

        try {
            // 调用Python微服务的Gemini Vision OCR功能
            // 读取文件
            byte[] fileBytes = Files.readAllBytes(filePath);
            log.info("文件读取成功，大小: {} bytes", fileBytes.length);

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
            
            // ========== 关键修复：明确传递Vision OCR专用参数（字符串格式） ==========
            body.add("use_pypdf2", "false");      // Vision OCR模式下禁用PyPDF2
            body.add("use_docling", "false");     // Vision OCR模式下禁用Docling
            body.add("use_gemini", "false");      // Vision OCR模式下禁用常规Gemini分析
            body.add("force_ocr", "false");       // 不强制OCR
            body.add("language", language);
            body.add("gemini_model", geminiModel);
            body.add("use_vision_ocr", "true");   // 启用Vision OCR模式

            // 详细日志记录发送的参数
            log.info("=== Vision OCR 请求参数 ===");
            log.info("use_pypdf2: false");
            log.info("use_docling: false");
            log.info("use_gemini: false");
            log.info("use_vision_ocr: true");
            log.info("force_ocr: false");
            log.info("language: {}", language);
            log.info("gemini_model: {}", geminiModel);
            log.info("file: {}", filePath.getFileName().toString());
            log.info("===========================");

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("开始调用Python微服务，URL: {}", doclingUrl);
            
            // 调用Docling OCR服务（它现在支持Gemini Vision OCR）
            ResponseEntity<String> response = restTemplate.postForEntity(
                doclingUrl,
                requestEntity,
                String.class
            );

            log.info("Python微服务响应状态码: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 解析JSON响应
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                log.info("成功解析Python微服务响应JSON");

                Map<String, Object> result = new HashMap<>();

                // Python微服务返回的是OcrResponse结构，需要从full_text字段提取文本
                String extractedText = jsonNode.path("full_text").asText("");

                // 如果full_text为空，尝试从gemini_analysis中获取
                if (extractedText.isEmpty()) {
                    JsonNode geminiAnalysis = jsonNode.path("gemini_analysis");
                    if (!geminiAnalysis.isMissingNode()) {
                        extractedText = geminiAnalysis.path("raw_response").asText("");
                        log.info("从gemini_analysis.raw_response获取文本，长度: {}", extractedText.length());
                    }
                }

                // 如果仍然为空，尝试从pages中获取文本
                if (extractedText.isEmpty()) {
                    JsonNode pages = jsonNode.path("pages");
                    if (pages.isArray()) {
                        StringBuilder textBuilder = new StringBuilder();
                        for (JsonNode page : pages) {
                            String pageText = page.path("text").asText("");
                            if (!pageText.isEmpty()) {
                                textBuilder.append(pageText).append("\n");
                            }
                        }
                        extractedText = textBuilder.toString().trim();
                        log.info("从pages数组获取文本，长度: {}", extractedText.length());
                    }
                }

                result.put("extractedText", extractedText);
                result.put("analysis", jsonNode.path("gemini_analysis").path("raw_response").asText(""));
                result.put("method", "Gemini Vision OCR");

                log.info("Gemini Vision OCR处理成功: {}, 提取文本长度: {}", filePath, extractedText.length());
                
                // 如果文本为空，记录详细的响应信息以便调试
                if (extractedText.isEmpty()) {
                    log.warn("Vision OCR未能提取到文本，Python响应结构:");
                    log.warn("full_text: {}", jsonNode.path("full_text").asText("(empty)"));
                    log.warn("gemini_analysis.raw_response: {}", jsonNode.path("gemini_analysis").path("raw_response").asText("(empty)"));
                    log.warn("pages数量: {}", jsonNode.path("pages").size());
                }
                
                return CompletableFuture.completedFuture(result);
            } else {
                String errorMsg = String.format("Gemini Vision OCR调用失败 - 状态码: %s, 响应: %s", 
                    response.getStatusCode(), response.getBody());
                log.error(errorMsg);
                return CompletableFuture.completedFuture(Map.of("error", errorMsg));
            }
        } catch (Exception e) {
            String errorMsg = "Gemini Vision OCR处理异常: " + e.getMessage();
            log.error("Gemini Vision OCR处理异常: {}", filePath, e);
            return CompletableFuture.completedFuture(Map.of("error", errorMsg));
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
        String language = ((String) options.getOrDefault("language", "auto")).toLowerCase(); // 获取并转为小写，方便比较
        // String geminiModelForAnalysis = (String) options.getOrDefault("geminiModel", "gemini-1.5-flash");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", geminiApiKey);

            Map<String, Object> requestBody = new HashMap<>();

            String prompt;
            // 根据语言选择不同的自然语言提示词，明确要求非JSON输出
            if ("ja".equals(language)) {
                prompt = "提供されたOCR抽出テキストに基づいて、主要な内容の簡潔な要約といくつかの重要なポイントを挙げてください。回答はJSON形式ではなく、平易な日本語の文章で記述してください。\n\n以下がテキストです：\n" + text;
            } else if ("zh".equals(language)) {
                prompt = "根据提供的OCR提取文本，请提供主要内容的简洁摘要并列出几个关键点。请以通顺的简体中文纯文本形式回答，不要使用JSON格式。\n\n以下是文本内容：\n" + text;
            } else { // 默认或 "en" 或 "auto" 等其他情况，使用英文提示
                prompt = "Based on the provided OCR-extracted text, please provide a concise summary of the main content and list a few key points. Respond in plain, natural English language, not in JSON format.\n\nHere is the text:\n" + text;
            }
            log.info("Gemini分析提示词 (目标语言: {}): {}", language, prompt.substring(0, Math.min(prompt.length(), 200)) + "...");


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

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.5); // 稍微提高一点温度，允许更多创造性/自然语言
            generationConfig.put("topP", 0.9);
            generationConfig.put("topK", 40);
            // 根据需要调整maxOutputTokens，确保分析内容不会被截断
            // generationConfig.put("maxOutputTokens", 2048); 
            requestBody.put("generationConfig", generationConfig);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                geminiUrl,
                requestEntity,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String responseText = "";

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
                    // 即使格式不符合预期，也尝试将整个body作为文本处理
                    responseText = response.getBody() != null ? response.getBody() : "";
                     if (responseText.length() > 500) { // 避免过长的原始JSON错误信息
                        responseText = responseText.substring(0, 500) + "... (响应体过长或非预期格式)";
                    }
                    return CompletableFuture.completedFuture(Map.of("error", "Gemini API响应格式不符合预期", "raw_response_preview", responseText));
                }
                
                // 因为我们期望纯文本，所以直接将responseText包装起来
                // extractJsonFromText 的逻辑需要调整，或者这里直接处理
                log.info("Gemini分析成功，获取到响应文本 (长度: {})", responseText.length());
                // 直接返回包含纯文本分析结果的Map
                return CompletableFuture.completedFuture(Map.of("analysis_text", responseText.trim()));
            } else {
                log.error("Gemini分析失败，状态码: {}", response.getStatusCode());
                String errorBody = response.getBody() != null ? response.getBody() : "";
                if (errorBody.length() > 500) {
                     errorBody = errorBody.substring(0, 500) + "... (响应体过长)";
                }
                return CompletableFuture.completedFuture(Map.of("error", "Gemini API调用失败: " + response.getStatusCode(), "details", errorBody));
            }
        } catch (Exception e) {
            log.error("Gemini分析异常", e);
            return CompletableFuture.completedFuture(Map.of("error", "Gemini分析异常: " + e.getMessage()));
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

    /**
     * 判断文件是否为Excel文件
     *
     * @param fileName 文件名
     * @return 是否为Excel文件
     */
    private boolean isExcelFile(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".xlsx") ||
               lowerCaseFileName.endsWith(".xls") ||
               lowerCaseFileName.endsWith(".xlsm");
    }

    /**
     * 判断文件是否为Word文档
     *
     * @param fileName 文件名
     * @return 是否为Word文档
     */
    private boolean isWordFile(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".docx") ||
               lowerCaseFileName.endsWith(".doc");
    }

    /**
     * 判断文件是否为纯文本文件
     *
     * @param fileName 文件名
     * @return 是否为纯文本文件
     */
    private boolean isTextFile(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".txt") ||
               lowerCaseFileName.endsWith(".md") ||
               lowerCaseFileName.endsWith(".rtf");
    }

    /**
     * 判断文件是否为CSV文件
     *
     * @param fileName 文件名
     * @return 是否为CSV文件
     */
    private boolean isCsvFile(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        return lowerCaseFileName.endsWith(".csv") ||
               lowerCaseFileName.endsWith(".tsv");
    }

    /**
     * 从Excel文件中提取文本内容
     *
     * @param filePath Excel文件路径
     * @param language 语言代码
     * @param geminiModel Gemini模型选择
     * @return 提取的文本内容
     */
    @Async
    public CompletableFuture<String> extractTextFromExcel(Path filePath, String language, String geminiModel) {
        log.info("从Excel文件提取文本: {}", filePath);

        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            Workbook workbook = null;
            String fileName = filePath.getFileName().toString().toLowerCase();

            // 根据文件扩展名选择合适的工作簿类型
            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xlsm")) {
                workbook = new XSSFWorkbook(fis);
            } else if (fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(fis);
            } else {
                throw new UnsupportedOperationException("不支持的Excel文件格式: " + fileName);
            }

            StringBuilder textBuilder = new StringBuilder();

            // 遍历所有工作表
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();

                log.debug("处理工作表: {}", sheetName);
                textBuilder.append("=== 工作表: ").append(sheetName).append(" ===\n");

                // 遍历所有行
                for (Row row : sheet) {
                    StringBuilder rowText = new StringBuilder();
                    boolean hasContent = false;

                    // 遍历行中的所有单元格
                    for (Cell cell : row) {
                        String cellValue = getCellValueAsString(cell);
                        if (cellValue != null && !cellValue.trim().isEmpty()) {
                            if (hasContent) {
                                rowText.append("\t");
                            }
                            rowText.append(cellValue);
                            hasContent = true;
                        }
                    }

                    // 如果行有内容，添加到文本中
                    if (hasContent) {
                        textBuilder.append(rowText.toString()).append("\n");
                    }
                }

                textBuilder.append("\n");
            }

            workbook.close();

            String extractedText = textBuilder.toString().trim();
            log.info("Excel文本提取成功: {}, 提取文本长度: {}", filePath, extractedText.length());

            return CompletableFuture.completedFuture(extractedText);

        } catch (Exception e) {
            log.error("Excel文本提取失败: {}", filePath, e);
            return CompletableFuture.completedFuture("");
        }
    }

    /**
     * 获取单元格的字符串值
     *
     * @param cell 单元格
     * @return 单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 避免科学计数法显示
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception e2) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
            case _NONE:
            default:
                return "";
        }
    }

    /**
     * 从Word文档中提取文本内容
     *
     * @param filePath Word文档路径
     * @param language 语言代码
     * @param geminiModel Gemini模型选择
     * @return 提取的文本内容
     */
    @Async
    public CompletableFuture<String> extractTextFromWord(Path filePath, String language, String geminiModel) {
        log.info("从Word文档提取文本: {}", filePath);

        try {
            String fileName = filePath.getFileName().toString().toLowerCase();
            StringBuilder textBuilder = new StringBuilder();

            if (fileName.endsWith(".docx")) {
                // 处理新版Word文档 (.docx)
                try (FileInputStream fis = new FileInputStream(filePath.toFile());
                     XWPFDocument document = new XWPFDocument(fis)) {

                    // 提取段落文本
                    for (XWPFParagraph paragraph : document.getParagraphs()) {
                        String paragraphText = paragraph.getText();
                        if (paragraphText != null && !paragraphText.trim().isEmpty()) {
                            textBuilder.append(paragraphText).append("\n");
                        }
                    }

                    // 提取表格文本
                    for (XWPFTable table : document.getTables()) {
                        textBuilder.append("\n=== 表格 ===\n");
                        for (XWPFTableRow row : table.getRows()) {
                            StringBuilder rowText = new StringBuilder();
                            for (XWPFTableCell cell : row.getTableCells()) {
                                String cellText = cell.getText();
                                if (cellText != null && !cellText.trim().isEmpty()) {
                                    if (rowText.length() > 0) {
                                        rowText.append("\t");
                                    }
                                    rowText.append(cellText);
                                }
                            }
                            if (rowText.length() > 0) {
                                textBuilder.append(rowText.toString()).append("\n");
                            }
                        }
                        textBuilder.append("\n");
                    }
                }
            } else if (fileName.endsWith(".doc")) {
                // 处理旧版Word文档 (.doc)
                try (FileInputStream fis = new FileInputStream(filePath.toFile());
                     HWPFDocument document = new HWPFDocument(fis);
                     WordExtractor extractor = new WordExtractor(document)) {

                    String text = extractor.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        textBuilder.append(text);
                    }
                }
            } else {
                throw new UnsupportedOperationException("不支持的Word文档格式: " + fileName);
            }

            String extractedText = textBuilder.toString().trim();
            log.info("Word文档文本提取成功: {}, 提取文本长度: {}", filePath, extractedText.length());

            return CompletableFuture.completedFuture(extractedText);

        } catch (Exception e) {
            log.error("Word文档文本提取失败: {}", filePath, e);
            return CompletableFuture.completedFuture("");
        }
    }

    /**
     * 从纯文本文件中提取文本内容
     *
     * @param filePath 文本文件路径
     * @return 提取的文本内容
     */
    @Async
    public CompletableFuture<String> extractTextFromTextFile(Path filePath) {
        log.info("从文本文件提取内容: {}", filePath);

        try {
            // 尝试使用UTF-8编码读取
            String text = Files.readString(filePath, StandardCharsets.UTF_8);

            // 如果文本为空或包含乱码，尝试其他编码
            if (text.trim().isEmpty() || containsGarbledText(text)) {
                log.info("UTF-8读取失败，尝试使用系统默认编码");
                text = Files.readString(filePath);
            }

            log.info("文本文件读取成功: {}, 文本长度: {}", filePath, text.length());
            return CompletableFuture.completedFuture(text);

        } catch (Exception e) {
            log.error("文本文件读取失败: {}", filePath, e);
            return CompletableFuture.completedFuture("");
        }
    }

    /**
     * 从CSV文件中提取文本内容
     *
     * @param filePath CSV文件路径
     * @return 提取的文本内容
     */
    @Async
    public CompletableFuture<String> extractTextFromCsv(Path filePath) {
        log.info("从CSV文件提取内容: {}", filePath);

        try {
            String fileName = filePath.getFileName().toString().toLowerCase();
            String delimiter = fileName.endsWith(".tsv") ? "\t" : ",";

            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            StringBuilder textBuilder = new StringBuilder();

            textBuilder.append("=== CSV/TSV 数据 ===\n");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line != null && !line.trim().isEmpty()) {
                    // 简单的CSV解析（不处理引号内的逗号）
                    String[] fields = line.split(delimiter);
                    StringBuilder rowText = new StringBuilder();

                    for (String field : fields) {
                        if (rowText.length() > 0) {
                            rowText.append(" | ");
                        }
                        rowText.append(field.trim());
                    }

                    if (i == 0) {
                        textBuilder.append("列标题: ").append(rowText.toString()).append("\n");
                    } else {
                        textBuilder.append("第").append(i).append("行: ").append(rowText.toString()).append("\n");
                    }
                }
            }

            String extractedText = textBuilder.toString().trim();
            log.info("CSV文件解析成功: {}, 提取文本长度: {}", filePath, extractedText.length());

            return CompletableFuture.completedFuture(extractedText);

        } catch (Exception e) {
            log.error("CSV文件解析失败: {}", filePath, e);
            return CompletableFuture.completedFuture("");
        }
    }

    /**
     * 检查文本是否包含乱码
     *
     * @param text 要检查的文本
     * @return 是否包含乱码
     */
    private boolean containsGarbledText(String text) {
        // 简单的乱码检测：检查是否包含大量替换字符
        long replacementCharCount = text.chars()
            .filter(ch -> ch == 0xFFFD) // Unicode替换字符
            .count();

        return replacementCharCount > text.length() * 0.1; // 如果超过10%是替换字符，认为是乱码
    }
}
