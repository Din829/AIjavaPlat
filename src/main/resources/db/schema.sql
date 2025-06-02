-- =====================================================
-- 社内業務サポートAIプラットフォーム 数据库初始化脚本
-- =====================================================
-- 此脚本用于创建应用所需的数据库表结构
-- 使用IF NOT EXISTS语句确保表不会被重复创建
-- 使用外键约束确保数据完整性
-- 使用自动时间戳记录创建和更新时间

-- 用户表
-- 存储系统用户信息，包括登录凭证
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,                                    -- 用户ID，自增主键
    username VARCHAR(50) NOT NULL UNIQUE,                                    -- 用户名，唯一，用于登录
    email VARCHAR(100) NOT NULL UNIQUE,                                      -- 电子邮件，唯一，可用于找回密码
    password VARCHAR(255) NOT NULL,                                          -- 密码哈希值，不存储明文密码
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,                  -- 创建时间，自动设置为当前时间
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  -- 更新时间，自动更新
);

-- API Token表
-- 存储用户的AI服务API令牌
CREATE TABLE IF NOT EXISTS api_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,                                    -- Token ID，自增主键
    user_id BIGINT NOT NULL,                                                 -- 用户ID，外键关联users表
    provider VARCHAR(50) NOT NULL,                                           -- 服务提供商，如"openai"
    token_value VARCHAR(255) NOT NULL,                                       -- 加密存储的Token值
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,                  -- 创建时间
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 更新时间
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE             -- 外键约束，用户删除时级联删除Token
);

-- Prompt表
-- 存储用户创建的AI提示模板
CREATE TABLE IF NOT EXISTS prompts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,                                    -- Prompt ID，自增主键
    user_id BIGINT NOT NULL,                                                 -- 用户ID，外键关联users表
    title VARCHAR(100) NOT NULL,                                             -- Prompt标题
    content TEXT NOT NULL,                                                   -- Prompt内容
    category VARCHAR(50),                                                    -- 分类，可为空
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,                  -- 创建时间
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 更新时间
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE             -- 外键约束，用户删除时级联删除Prompt
);

-- OCR任务表
-- 存储用户的OCR处理任务信息
CREATE TABLE IF NOT EXISTS ocr_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,                                    -- 任务ID，自增主键
    user_id BIGINT NOT NULL,                                                 -- 用户ID，外键关联users表
    task_id VARCHAR(36) NOT NULL UNIQUE,                                     -- 唯一任务ID（UUID格式）
    status VARCHAR(20) NOT NULL,                                             -- 任务状态：PENDING, PROCESSING, COMPLETED, FAILED
    original_filename VARCHAR(255),                                          -- 原始文件名
    stored_filename VARCHAR(255),                                            -- 存储的文件名（如果需要本地存储）
    result_json LONGTEXT,                                                    -- OCR结果（JSON字符串）
    error_message TEXT,                                                      -- 错误信息（如果有）
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,                  -- 创建时间
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 更新时间
    completed_at DATETIME,                                                   -- 完成时间
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE             -- 外键约束，用户删除时级联删除任务
);

-- 视频转写任务表
-- 存储用户的链接处理任务信息（网页摘要和视频转写）
CREATE TABLE IF NOT EXISTS video_transcription_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,                                    -- 任务ID，自增主键
    user_id BIGINT NOT NULL,                                                 -- 用户ID，外键关联users表
    task_id VARCHAR(36) NOT NULL UNIQUE,                                     -- 唯一任务ID（UUID格式）
    url VARCHAR(2048) NOT NULL,                                              -- 处理的URL链接
    content_type VARCHAR(20) NOT NULL,                                       -- 内容类型：WEBPAGE, VIDEO
    status VARCHAR(20) NOT NULL,                                             -- 任务状态：PENDING, PROCESSING, COMPLETED, FAILED

    -- 视频相关字段
    video_title VARCHAR(500),                                                -- 视频标题
    video_description TEXT,                                                  -- 视频描述
    video_duration INTEGER,                                                  -- 视频时长（秒）

    -- 处理选项
    language VARCHAR(10) DEFAULT 'auto',                                     -- 语言选择，默认自动检测
    custom_prompt TEXT,                                                      -- 自定义prompt

    -- 结果字段
    result_json LONGTEXT,                                                    -- 存储完整的处理结果（JSON格式）
    transcription_text LONGTEXT,                                            -- 转写文本（仅视频）
    summary_text TEXT,                                                       -- AI总结文本

    -- 时间字段
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,                  -- 创建时间
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,  -- 更新时间
    completed_at DATETIME,                                                   -- 完成时间

    -- 错误处理
    error_message TEXT,                                                      -- 错误信息（如果有）

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE             -- 外键约束，用户删除时级联删除任务
);
