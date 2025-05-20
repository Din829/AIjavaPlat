package com.ding.aiplatjava.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OCR响应DTO
 * 用于返回OCR任务的状态和结果
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 不序列化null值
public class OcrResponseDto {
    
    /**
     * 任务ID（UUID）
     */
    private String taskId;
    
    /**
     * 任务状态：PENDING, PROCESSING, COMPLETED, FAILED
     */
    private String status;
    
    /**
     * 原始文件名
     */
    private String originalFilename;
    
    /**
     * 创建时间（ISO格式字符串）
     */
    private String createdAt;
    
    /**
     * 完成时间（ISO格式字符串），仅当状态为COMPLETED时有值
     */
    private String completedAt;
    
    /**
     * OCR结果（JSON对象），仅当状态为COMPLETED时有值
     */
    private Object result;
    
    /**
     * 错误信息，仅当状态为FAILED时有值
     */
    private String errorMessage;
    
    /**
     * 构造函数 - 用于创建任务响应
     * 
     * @param taskId 任务ID
     * @param status 任务状态
     */
    public OcrResponseDto(String taskId, String status) {
        this.taskId = taskId;
        this.status = status;
    }
    
    /**
     * 构造函数 - 用于创建错误响应
     * 
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    public OcrResponseDto(String taskId, String status, String errorMessage) {
        this.taskId = taskId;
        this.status = status;
        this.errorMessage = errorMessage;
    }
}
