package com.ding.aiplatjava.entity;

import java.time.LocalDateTime;

/**
 * API Token实体类
 * 存储用户的AI服务API Token信息
 */
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
    
    /**
     * 默认构造函数
     */
    public ApiToken() {
    }
    
    /**
     * 全参数构造函数
     */
    public ApiToken(Long id, Long userId, String provider, String tokenValue, 
                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.tokenValue = tokenValue;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
} 