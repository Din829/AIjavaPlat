-- =====================================================
-- 社内業務サポートAIプラットフォーム 初始化数据脚本
-- =====================================================
-- 此脚本用于插入应用的初始化数据，包括测试用户等

-- 插入测试用户
-- 注意：密码需要使用BCrypt加密，这里使用的是"password123"的BCrypt哈希值
-- 实际应用中，密码应该通过注册接口或管理界面设置
INSERT IGNORE INTO users (username, email, password, created_at, updated_at) VALUES
('testuser', 'test@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', NOW(), NOW()),
('admin', 'admin@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', NOW(), NOW()),
('ding2', 'ding2@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', NOW(), NOW());

-- 注意：
-- 1. 使用 INSERT IGNORE 避免重复插入
-- 2. 所有测试用户的密码都是 "password123"
-- 3. 在生产环境中，应该删除或修改这些测试用户
-- 4. BCrypt哈希值 $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi 对应密码 "password123"
