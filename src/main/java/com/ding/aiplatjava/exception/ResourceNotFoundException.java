package com.ding.aiplatjava.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 资源未找到异常
 * 当请求的资源在系统中不存在时抛出此异常
 *
 * 使用场景：
 * - 根据ID查询用户，但用户不存在
 * - 根据ID查询API Token，但Token不存在
 * - 根据ID查询Prompt，但Prompt不存在
 * 等等
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // 标记此异常对应HTTP 404状态码
public class ResourceNotFoundException extends RuntimeException {

    /**
     * 使用自定义消息创建异常
     *
     * @param message 异常消息
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * 使用资源名称、字段名和字段值创建格式化的异常消息
     *
     * @param resourceName 资源名称，如"User"、"ApiToken"等
     * @param fieldName 字段名称，如"id"、"username"等
     * @param fieldValue 字段值，如用户ID、用户名等
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // 创建格式化的错误消息，如"User not found with id : '1'"
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}
