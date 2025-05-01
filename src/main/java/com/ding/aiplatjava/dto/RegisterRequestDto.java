package com.ding.aiplatjava.dto;

import lombok.Data;

/**
 * 注册请求的数据传输对象。
 */
@Data
public class RegisterRequestDto {
    private String username;
    private String email;
    private String password;
    private String confirmPassword;
} 