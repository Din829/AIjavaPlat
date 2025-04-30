package com.ding.aiplatjava.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API Token 数据传输对象 (DTO)
 * 用于 API 交互，与实体类 ApiToken 不同，通常不包含加密的 tokenValue。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiTokenDto {

    /**
     * Token ID (用于标识，更新或删除时可能用到)
     */
    private Long id;

    /**
     * 用户ID (信息展示用)
     */
    private Long userId;

    /**
     * AI 服务提供商 (例如 "openai", "google-ai")
     * 创建时需要提供。
     */
    private String provider;

    /**
     * 明文 Token 值。
     * 仅在创建 Token 的请求中使用，**绝不**应该在响应中返回。
     */
    private String tokenValue; // 只在请求体中使用

    /**
     * 创建时间 (信息展示用)
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间 (信息展示用)
     */
    private LocalDateTime updatedAt;
} 