package com.ding.aiplatjava.mapper;

import com.ding.aiplatjava.entity.OcrTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OCR任务数据访问接口
 * 提供对ocr_tasks表的CRUD操作
 */
@Mapper
public interface OcrTaskMapper {
    
    /**
     * 插入新的OCR任务
     * 
     * @param ocrTask OCR任务对象
     * @return 影响的行数
     */
    int insert(OcrTask ocrTask);
    
    /**
     * 根据ID查询OCR任务
     * 
     * @param id 任务ID
     * @return OCR任务对象，如果不存在则返回null
     */
    OcrTask selectById(Long id);
    
    /**
     * 根据任务ID（UUID）查询OCR任务
     * 
     * @param taskId 任务ID（UUID）
     * @return OCR任务对象，如果不存在则返回null
     */
    OcrTask selectByTaskId(String taskId);
    
    /**
     * 根据用户ID查询该用户的所有OCR任务
     * 
     * @param userId 用户ID
     * @return OCR任务列表，如果不存在则返回空列表
     */
    List<OcrTask> selectByUserId(Long userId);
    
    /**
     * 更新任务状态
     * 
     * @param taskId 任务ID（UUID）
     * @param status 新状态
     * @return 影响的行数
     */
    int updateStatus(@Param("taskId") String taskId, @Param("status") String status);
    
    /**
     * 更新任务结果
     * 
     * @param taskId 任务ID（UUID）
     * @param resultJson 结果JSON字符串
     * @param completedAt 完成时间
     * @return 影响的行数
     */
    int updateResult(@Param("taskId") String taskId, @Param("resultJson") String resultJson, @Param("completedAt") LocalDateTime completedAt);
    
    /**
     * 更新任务错误信息
     * 
     * @param taskId 任务ID（UUID）
     * @param errorMessage 错误信息
     * @return 影响的行数
     */
    int updateError(@Param("taskId") String taskId, @Param("errorMessage") String errorMessage);
}
