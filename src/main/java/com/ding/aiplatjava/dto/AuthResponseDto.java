package com.ding.aiplatjava.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应的数据传输对象，包含访问令牌。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String accessToken;
} 