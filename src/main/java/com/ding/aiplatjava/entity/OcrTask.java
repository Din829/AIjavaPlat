package com.ding.aiplatjava.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * OCR任务实体类
 * 对应数据库中的ocr_tasks表，用于存储用户的OCR处理任务信息
 */
@Data
public class OcrTask {
    /**
     * 任务ID，自增主键
     */
    private Long id;
    
    /**
     * 用户ID，关联users表
     */
    private Long userId;
    
    /**
     * 唯一任务ID（UUID格式），用于前端查询任务状态
     */
    private String taskId;
    
    /**
     * 任务状态：PENDING（等待处理）, PROCESSING（处理中）, COMPLETED（已完成）, FAILED（失败）
     */
    private String status;
    
    /**
     * 原始文件名，用户上传的文件名
     */
    private String originalFilename;
    
    /**
     * 存储的文件名（如果需要本地存储）
     */
    private String storedFilename;
    
    /**
     * OCR结果（JSON字符串）
     */
    private String resultJson;
    
    /**
     * 错误信息（如果有）
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime completedAt;
}
