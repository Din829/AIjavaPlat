package com.ding.aiplatjava.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册数据传输对象
 * 用于接收客户端提交的用户注册信息
 *
 * 特点：
 * 1. 包含用户注册所需的所有字段
 * 2. 包含密码和确认密码字段，用于验证
 * 3. 在控制器中会被转换为User实体后再存储到数据库
 */
@Data               // Lombok注解，自动生成getter、setter、equals、hashCode和toString方法
@NoArgsConstructor  // 自动生成无参构造函数
@AllArgsConstructor // 自动生成包含所有字段的构造函数
@Builder            // 自动生成建造者模式代码，方便对象创建
public class UserRegistrationDto {

    /**
     * 用户名
     * 用户登录系统的唯一标识
     */
    private String username;

    /**
     * 电子邮件
     * 用于联系用户和找回密码
     */
    private String email;

    /**
     * 密码
     * 用户的登录凭证，需要进行加密处理后再存储
     */
    private String password;

    /**
     * 确认密码
     * 用于验证用户输入的密码是否一致，防止输入错误
     * 注意：此字段仅用于验证，不会存储到数据库
     */
    private String confirmPassword;

    // 可以根据需要添加其他注册信息，如：
    // private String fullName;
    // private String phoneNumber;
    // private boolean agreeToTerms;
}
