package com.ding.aiplatjava.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Prompt实体类
 * 存储用户创建的Prompt信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prompt {

    /**
     * Prompt ID (主键)
     */
    private Long id;

    /**
     * 用户ID (外键，关联 users.id)
     */
    private Long userId;

    /**
     * Prompt标题
     */
    private String title;

    /**
     * Prompt内容
     */
    private String content;

    /**
     * Prompt分类
     */
    private String category;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
} 