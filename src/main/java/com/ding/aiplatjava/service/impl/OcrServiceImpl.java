package com.ding.aiplatjava.service.impl;

import com.ding.aiplatjava.dto.OcrResponseDto;
import com.ding.aiplatjava.dto.OcrTaskStatusDto;
import com.ding.aiplatjava.dto.OcrUploadRequestDto;
import com.ding.aiplatjava.entity.OcrTask;
import com.ding.aiplatjava.exception.ResourceNotFoundException;
import com.ding.aiplatjava.mapper.OcrTaskMapper;
import com.ding.aiplatjava.service.OcrProcessingService;
import com.ding.aiplatjava.service.OcrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * OCR服务实现类
 * 实现OCR相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class OcrServiceImpl implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrServiceImpl.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final OcrTaskMapper ocrTaskMapper;
    private final OcrProcessingService ocrProcessingService;
    private final ObjectMapper objectMapper;

    @Value("${ocr.upload.dir:${java.io.tmpdir}}")
    private String uploadDir;

    /**
     * 上传并处理文件
     *
     * @param file 上传的文件
     * @param userId 用户ID
     * @param requestDto 请求参数
     * @return OCR响应DTO，包含任务ID和初始状态
     */
    @Override
    public OcrResponseDto uploadAndProcess(MultipartFile file, Long userId, OcrUploadRequestDto requestDto) {
        log.info("开始处理OCR上传请求，用户ID: {}, 文件名: {}", userId, file.getOriginalFilename());

        try {
            // 1. 生成唯一任务ID
            String taskId = UUID.randomUUID().toString();
            log.debug("生成任务ID: {}", taskId);

            // 2. 保存文件到临时目录
            String originalFilename = file.getOriginalFilename();
            String storedFilename = taskId + "_" + originalFilename;
            Path filePath = Paths.get(uploadDir, storedFilename);
            Files.createDirectories(filePath.getParent());
            Files.copy(file.getInputStream(), filePath);
            log.debug("文件已保存到: {}", filePath);

            // 3. 创建任务记录
            OcrTask task = new OcrTask();
            task.setUserId(userId);
            task.setTaskId(taskId);
            task.setStatus("PENDING");
            task.setOriginalFilename(originalFilename);
            task.setStoredFilename(storedFilename);
            task.setCreatedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());

            ocrTaskMapper.insert(task);
            log.debug("任务记录已创建，ID: {}", task.getId());

            // 4. 异步处理OCR任务
            processOcrTaskAsync(taskId, filePath.toString(), userId, requestDto);

            // 5. 返回初始响应
            OcrResponseDto responseDto = new OcrResponseDto(taskId, "PENDING");
            responseDto.setOriginalFilename(originalFilename);
            responseDto.setCreatedAt(task.getCreatedAt().format(DATE_TIME_FORMATTER));

            log.info("OCR上传请求处理完成，任务ID: {}", taskId);
            return responseDto;

        } catch (IOException e) {
            log.error("处理OCR上传请求时发生错误", e);
            return new OcrResponseDto(null, "FAILED", "文件处理失败: " + e.getMessage());
        }
    }

    /**
     * 异步处理OCR任务
     *
     * @param taskId 任务ID
     * @param filePath 文件路径
     * @param userId 用户ID
     * @param requestDto 请求参数
     * @return 异步任务结果
     */
    @Override
    @Async
    public CompletableFuture<OcrResponseDto> processOcrTaskAsync(String taskId, String filePath, Long userId, OcrUploadRequestDto requestDto) {
        // 转换为内部方法调用
        processOcrTaskInternally(taskId, Paths.get(filePath), userId, requestDto);
        // 返回一个空的CompletableFuture，因为实际处理是异步的
        return CompletableFuture.completedFuture(new OcrResponseDto(taskId, "PROCESSING"));
    }

    /**
     * 内部异步处理OCR任务
     *
     * @param taskId 任务ID
     * @param filePath 文件路径
     * @param userId 用户ID
     * @param requestDto 请求参数
     */
    @Async
    protected void processOcrTaskInternally(String taskId, Path filePath, Long userId, OcrUploadRequestDto requestDto) {
        log.info("开始异步处理OCR任务: {}", taskId);

        try {
            // 1. 更新任务状态为处理中
            ocrTaskMapper.updateStatus(taskId, "PROCESSING");

            // 2. 准备处理选项
            Map<String, Object> options = new HashMap<>();
            options.put("usePypdf2", requestDto.isUsePypdf2());
            options.put("useDocling", requestDto.isUseDocling());
            options.put("useGemini", requestDto.isUseGemini());
            options.put("forceOcr", requestDto.isForceOcr());
            options.put("language", requestDto.getLanguage());

            // 3. 调用OCR处理服务
            CompletableFuture<Map<String, Object>> resultFuture = ocrProcessingService.processFile(filePath, options);

            // 4. 处理结果
            Map<String, Object> result = resultFuture.get(); // 等待处理完成

            if (result.containsKey("error")) {
                // 处理失败
                String errorMessage = (String) result.get("error");
                ocrTaskMapper.updateError(taskId, errorMessage);
                log.error("OCR处理失败: {}, 错误: {}", taskId, errorMessage);
                return;
            }

            // 5. 处理成功，更新任务状态
            LocalDateTime completedAt = LocalDateTime.now();
            String resultJson = objectMapper.writeValueAsString(result);

            // 记录结果JSON的前200个字符，用于调试
            String resultSummary = resultJson.length() > 200
                ? resultJson.substring(0, 200) + "..."
                : resultJson;
            log.info("OCR处理结果: {}, 结果摘要: {}", taskId, resultSummary);

            // 更新数据库
            int updateCount = ocrTaskMapper.updateResult(taskId, resultJson, completedAt);
            log.info("OCR任务处理成功: {}, 更新记录数: {}", taskId, updateCount);

            // 验证更新是否成功
            OcrTask updatedTask = ocrTaskMapper.selectByTaskId(taskId);
            if (updatedTask != null) {
                log.info("更新后的任务状态: {}, 完成时间: {}, 结果JSON长度: {}",
                    updatedTask.getStatus(),
                    updatedTask.getCompletedAt(),
                    updatedTask.getResultJson() != null ? updatedTask.getResultJson().length() : 0);
            } else {
                log.warn("无法获取更新后的任务: {}", taskId);
            }

            // 6. 清理临时文件（可选）
            // Files.deleteIfExists(filePath);

        } catch (InterruptedException | ExecutionException e) {
            // 处理异步执行异常
            ocrTaskMapper.updateError(taskId, "OCR处理异常: " + e.getMessage());
            log.error("OCR异步处理异常: {}", taskId, e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // 处理其他异常
            ocrTaskMapper.updateError(taskId, e.getMessage());
            log.error("处理OCR任务时发生错误: {}", taskId, e);
        }
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务状态DTO
     */
    @Override
    public OcrTaskStatusDto getTaskStatus(String taskId, Long userId) {
        log.debug("获取任务状态，任务ID: {}, 用户ID: {}", taskId, userId);

        OcrTask task = ocrTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            throw new ResourceNotFoundException("OCR任务", "taskId", taskId);
        }

        // 验证用户权限
        if (!task.getUserId().equals(userId)) {
            log.warn("用户无权访问此任务，任务ID: {}, 用户ID: {}", taskId, userId);
            throw new ResourceNotFoundException("OCR任务", "taskId", taskId);
        }

        String message;
        switch (task.getStatus()) {
            case "PENDING":
                message = "任务等待处理";
                break;
            case "PROCESSING":
                message = "任务处理中";
                break;
            case "COMPLETED":
                message = "任务已完成";
                break;
            case "FAILED":
                message = task.getErrorMessage() != null ? task.getErrorMessage() : "任务处理失败";
                break;
            default:
                message = "未知状态";
        }

        return new OcrTaskStatusDto(taskId, task.getStatus(), message);
    }

    /**
     * 获取任务结果
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return OCR响应DTO，包含任务结果
     */
    @Override
    public OcrResponseDto getTaskResult(String taskId, Long userId) {
        log.debug("获取任务结果，任务ID: {}, 用户ID: {}", taskId, userId);

        OcrTask task = ocrTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            log.warn("任务不存在: {}", taskId);
            throw new ResourceNotFoundException("OCR任务", "taskId", taskId);
        }

        // 验证用户权限
        if (!task.getUserId().equals(userId)) {
            log.warn("用户无权访问此任务，任务ID: {}, 用户ID: {}", taskId, userId);
            throw new ResourceNotFoundException("OCR任务", "taskId", taskId);
        }

        OcrResponseDto responseDto = new OcrResponseDto(taskId, task.getStatus());
        responseDto.setOriginalFilename(task.getOriginalFilename());
        responseDto.setCreatedAt(task.getCreatedAt().format(DATE_TIME_FORMATTER));

        if (task.getCompletedAt() != null) {
            responseDto.setCompletedAt(task.getCompletedAt().format(DATE_TIME_FORMATTER));
        }

        if ("COMPLETED".equals(task.getStatus()) && task.getResultJson() != null) {
            try {
                JsonNode resultJson = objectMapper.readTree(task.getResultJson());
                responseDto.setResult(resultJson);
            } catch (Exception e) {
                log.error("解析任务结果JSON时发生错误", e);
                responseDto.setStatus("FAILED");
                responseDto.setErrorMessage("解析结果失败: " + e.getMessage());
            }
        } else if ("FAILED".equals(task.getStatus())) {
            responseDto.setErrorMessage(task.getErrorMessage());
        }

        return responseDto;
    }

    /**
     * 获取用户的所有任务
     *
     * @param userId 用户ID
     * @return 任务列表
     */
    @Override
    public List<OcrResponseDto> getUserTasks(Long userId) {
        log.debug("获取用户任务列表，用户ID: {}", userId);

        List<OcrTask> tasks = ocrTaskMapper.selectByUserId(userId);
        List<OcrResponseDto> responseDtos = new ArrayList<>();

        for (OcrTask task : tasks) {
            OcrResponseDto dto = new OcrResponseDto(task.getTaskId(), task.getStatus());
            dto.setOriginalFilename(task.getOriginalFilename());
            dto.setCreatedAt(task.getCreatedAt().format(DATE_TIME_FORMATTER));

            if (task.getCompletedAt() != null) {
                dto.setCompletedAt(task.getCompletedAt().format(DATE_TIME_FORMATTER));
            }

            if ("FAILED".equals(task.getStatus())) {
                dto.setErrorMessage(task.getErrorMessage());
            }

            responseDtos.add(dto);
        }

        return responseDtos;
    }
}
