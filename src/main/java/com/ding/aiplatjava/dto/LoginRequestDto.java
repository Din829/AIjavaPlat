package com.ding.aiplatjava.dto;

import lombok.Data;

/**
 * 登录请求的数据传输对象。
 */
@Data
public class LoginRequestDto {
    private String username;
    private String password;
} 