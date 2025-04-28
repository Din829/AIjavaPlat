package com.ding.aiplatjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 社内業務サポートAIプラットフォーム (内部业务支持AI平台) 主应用类
 *
 * 这是Spring Boot应用的入口点，负责启动整个应用
 *
 * 项目概述：
 * 构建一个基于Web的平台，利用AI能力，为用户提供处理公开信息的辅助工具，
 * 提高个人工作效率。平台不处理任何公司内部或客户的敏感数据。
 * 用户需要提供自己的AI服务API Token来驱动AI功能。
 *
 * 核心功能模块：
 * 1. 用户认证与管理
 * 2. API Token安全管理
 * 3. Prompt管理
 * 4. 网页内容摘要
 */
@SpringBootApplication // 标记为Spring Boot应用，启用自动配置和组件扫描
public class AIplatJavaApplication {

    /**
     * 应用程序入口点
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 启动Spring Boot应用
        SpringApplication.run(AIplatJavaApplication.class, args);
    }

}
