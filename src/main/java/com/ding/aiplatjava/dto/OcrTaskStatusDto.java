package com.ding.aiplatjava.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OCR任务状态DTO
 * 用于返回OCR任务的状态信息
 */
@Data
@NoArgsConstructor
public class OcrTaskStatusDto {
    
    /**
     * 任务ID（UUID）
     */
    private String taskId;
    
    /**
     * 任务状态：PENDING, PROCESSING, COMPLETED, FAILED
     */
    private String status;
    
    /**
     * 处理进度（0-100）
     * 仅当状态为PROCESSING时有意义
     */
    private Integer progress;
    
    /**
     * 状态消息
     * 用于提供额外的状态信息
     */
    private String message;
    
    /**
     * 构造函数
     * 
     * @param taskId 任务ID
     * @param status 任务状态
     * @param message 状态消息
     */
    public OcrTaskStatusDto(String taskId, String status, String message) {
        this.taskId = taskId;
        this.status = status;
        this.message = message;
    }
    
    /**
     * 构造函数
     * 
     * @param taskId 任务ID
     * @param status 任务状态
     * @param progress 处理进度
     * @param message 状态消息
     */
    public OcrTaskStatusDto(String taskId, String status, Integer progress, String message) {
        this.taskId = taskId;
        this.status = status;
        this.progress = progress;
        this.message = message;
    }
}
