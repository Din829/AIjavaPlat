package com.ding.aiplatjava.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象 (DTO - Data Transfer Object)
 * 用于在控制器和客户端之间传输用户数据
 *
 * 与实体类的区别：
 * 1. 不包含敏感信息（如密码）
 * 2. 可能包含额外的展示字段
 * 3. 可能合并多个实体的数据
 * 4. 专注于API接口的数据需求
 */
@Data               // Lombok注解，自动生成getter、setter、equals、hashCode和toString方法
@NoArgsConstructor  // 自动生成无参构造函数
@AllArgsConstructor // 自动生成包含所有字段的构造函数
@Builder            // 自动生成建造者模式代码，方便对象创建
public class UserDto {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 电子邮件
     */
    private String email;

    // 注意：不包含密码字段，保证安全

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // 可以根据需要添加其他非敏感字段，如：
    // private List<String> roles;
    // private boolean active;
    // private String displayName;
}
