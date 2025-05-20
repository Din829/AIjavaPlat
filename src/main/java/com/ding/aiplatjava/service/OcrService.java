package com.ding.aiplatjava.service;

import com.ding.aiplatjava.dto.OcrResponseDto;
import com.ding.aiplatjava.dto.OcrTaskStatusDto;
import com.ding.aiplatjava.dto.OcrUploadRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * OCR服务接口
 * 提供OCR相关的业务逻辑
 */
public interface OcrService {
    
    /**
     * 上传并处理文件
     * 
     * @param file 上传的文件
     * @param userId 用户ID
     * @param requestDto 请求参数
     * @return OCR响应DTO，包含任务ID和初始状态
     */
    OcrResponseDto uploadAndProcess(MultipartFile file, Long userId, OcrUploadRequestDto requestDto);
    
    /**
     * 异步处理OCR任务
     * 
     * @param taskId 任务ID
     * @param filePath 文件路径
     * @param userId 用户ID
     * @param requestDto 请求参数
     * @return 异步任务结果
     */
    CompletableFuture<OcrResponseDto> processOcrTaskAsync(String taskId, String filePath, Long userId, OcrUploadRequestDto requestDto);
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务状态DTO
     */
    OcrTaskStatusDto getTaskStatus(String taskId, Long userId);
    
    /**
     * 获取任务结果
     * 
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return OCR响应DTO，包含任务结果
     */
    OcrResponseDto getTaskResult(String taskId, Long userId);
    
    /**
     * 获取用户的所有任务
     * 
     * @param userId 用户ID
     * @return 任务列表
     */
    List<OcrResponseDto> getUserTasks(Long userId);
}
