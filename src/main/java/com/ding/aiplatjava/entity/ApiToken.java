package com.ding.aiplatjava.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API Token实体类
 * 存储用户的AI服务API Token信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiToken {

    /**
     * Token ID (主键)
     */
    private Long id;

    /**
     * 用户ID (外键，关联 users.id)
     */
    private Long userId;

    /**
     * AI服务提供商 (例如 "openai")
     */
    private String provider;

    /**
     * 加密存储的API Token值
     */
    private String tokenValue;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
} 