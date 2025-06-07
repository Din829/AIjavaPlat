package com.ding.aiplatjava.mapper;

import com.ding.aiplatjava.entity.VideoTranscriptionTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频转写任务数据访问接口
 * 提供对video_transcription_tasks表的CRUD操作
 */
@Mapper
public interface VideoTranscriptionTaskMapper {

    /**
     * 插入新的视频转写任务
     * 
     * @param task 视频转写任务对象
     * @return 影响的行数
     */
    int insert(VideoTranscriptionTask task);

    /**
     * 根据ID查询视频转写任务
     * 
     * @param id 任务ID
     * @return 视频转写任务对象，如果不存在则返回null
     */
    VideoTranscriptionTask selectById(Long id);

    /**
     * 根据任务ID（UUID）查询视频转写任务
     * 
     * @param taskId 任务ID（UUID）
     * @return 视频转写任务对象，如果不存在则返回null
     */
    VideoTranscriptionTask selectByTaskId(String taskId);

    /**
     * 根据用户ID查询该用户的所有视频转写任务
     * 
     * @param userId 用户ID
     * @return 视频转写任务列表，如果不存在则返回空列表
     */
    List<VideoTranscriptionTask> selectByUserId(Long userId);

    /**
     * 根据用户ID和任务ID查询视频转写任务
     * 用于安全检查，确保用户只能访问自己的任务
     * 
     * @param taskId 任务ID（UUID）
     * @param userId 用户ID
     * @return 视频转写任务对象，如果不存在或不属于该用户则返回null
     */
    VideoTranscriptionTask selectByTaskIdAndUserId(@Param("taskId") String taskId, @Param("userId") Long userId);

    /**
     * 更新任务状态
     * 
     * @param taskId 任务ID（UUID）
     * @param status 新状态
     * @return 影响的行数
     */
    int updateStatus(@Param("taskId") String taskId, @Param("status") String status);

    /**
     * 更新视频元数据信息
     * 
     * @param taskId 任务ID（UUID）
     * @param videoTitle 视频标题
     * @param videoDescription 视频描述
     * @param videoDuration 视频时长（秒），支持小数点精度
     * @return 影响的行数
     */
    int updateVideoMetadata(@Param("taskId") String taskId, 
                           @Param("videoTitle") String videoTitle, 
                           @Param("videoDescription") String videoDescription, 
                           @Param("videoDuration") Double videoDuration);

    /**
     * 更新任务结果
     * 
     * @param taskId 任务ID（UUID）
     * @param resultJson 结果JSON字符串
     * @param transcriptionText 转写文本
     * @param summaryText 总结文本
     * @param completedAt 完成时间
     * @return 影响的行数
     */
    int updateResult(@Param("taskId") String taskId, 
                    @Param("resultJson") String resultJson, 
                    @Param("transcriptionText") String transcriptionText, 
                    @Param("summaryText") String summaryText, 
                    @Param("completedAt") LocalDateTime completedAt);

    /**
     * 更新任务错误信息
     * 
     * @param taskId 任务ID（UUID）
     * @param errorMessage 错误信息
     * @return 影响的行数
     */
    int updateError(@Param("taskId") String taskId, @Param("errorMessage") String errorMessage);

    /**
     * 根据状态查询任务列表
     * 用于监控和管理任务
     * 
     * @param status 任务状态
     * @return 任务列表
     */
    List<VideoTranscriptionTask> selectByStatus(String status);

    /**
     * 删除指定用户的任务
     * 用于用户删除自己的任务记录
     * 
     * @param taskId 任务ID（UUID）
     * @param userId 用户ID
     * @return 影响的行数
     */
    int deleteByTaskIdAndUserId(@Param("taskId") String taskId, @Param("userId") Long userId);
}
