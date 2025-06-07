-- 数据库迁移脚本：修改视频时长字段类型支持小数精度
-- 从 INTEGER 改为 DOUBLE 类型
-- 适用于 MySQL 8.0+

-- 修改 video_transcription_tasks 表的 video_duration 字段
ALTER TABLE video_transcription_tasks 
MODIFY COLUMN video_duration DOUBLE COMMENT '视频时长（秒），支持小数点精度';

-- 验证修改结果
SHOW COLUMNS FROM video_transcription_tasks LIKE 'video_duration'; 