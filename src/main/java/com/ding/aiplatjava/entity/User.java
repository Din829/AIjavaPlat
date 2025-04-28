package com.ding.aiplatjava.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体类
 * 对应数据库中的users表，存储用户基本信息
 */
@Data               // Lombok注解，自动生成getter、setter、equals、hashCode和toString方法
@NoArgsConstructor  // 自动生成无参构造函数
@AllArgsConstructor // 自动生成包含所有字段的构造函数
@Builder            // 自动生成建造者模式代码，方便对象创建
public class User {

    /**
     * 用户ID
     * 数据库主键，自动递增
     */
    private Long id;

    /**
     * 用户名
     * 用户登录系统的唯一标识，不允许重复
     */
    private String username;

    /**
     * 电子邮件
     * 用户的联系方式，也可用于登录，不允许重复
     */
    private String email;

    /**
     * 密码
     * 用户的登录凭证，存储时需要加密
     * 注意：实际存储的是加密后的密码哈希值，而非明文密码
     */
    private String password;

    /**
     * 创建时间
     * 记录用户账号的创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     * 记录用户信息的最后更新时间
     */
    private LocalDateTime updatedAt;
}
