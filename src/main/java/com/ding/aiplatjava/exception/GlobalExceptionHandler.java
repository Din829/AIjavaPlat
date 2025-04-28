package com.ding.aiplatjava.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

/**
 * 全局异常处理器
 * 集中处理应用中抛出的各种异常，统一异常响应格式
 *
 * 主要功能：
 * 1. 捕获特定类型的异常并进行处理
 * 2. 将异常转换为统一的错误响应格式
 * 3. 设置适当的HTTP状态码
 * 4. 记录异常信息（可扩展）
 */

@ControllerAdvice // 标记为控制器增强，用于全局异常处理
public class GlobalExceptionHandler {

    /**
     * 处理资源未找到异常
     * 当系统中抛出ResourceNotFoundException时会被此方法捕获并处理
     *
     * @param ex 捕获到的ResourceNotFoundException异常
     * @param request 当前Web请求
     * @return 包含错误详情的ResponseEntity，状态码404
     */
    @ExceptionHandler(ResourceNotFoundException.class) // 指定要处理的异常类型
    public ResponseEntity<?> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        // 创建错误详情对象，包含时间戳、错误消息和请求描述
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false)); // false表示不包含客户端信息

        // 返回404状态码和错误详情
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    /**
     * 处理所有其他未明确处理的异常
     * 作为默认的异常处理器，捕获所有其他类型的异常
     *
     * @param ex 捕获到的Exception异常
     * @param request 当前Web请求
     * @return 包含错误详情的ResponseEntity，状态码500
     */
    @ExceptionHandler(Exception.class) // 处理所有类型的异常
    public ResponseEntity<?> handleGlobalException(
            Exception ex, WebRequest request) {

        // 创建错误详情对象
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                ex.getMessage(),
                request.getDescription(false));

        // 返回500状态码和错误详情
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 错误详情类
     * 定义了API错误响应的标准格式
     */
    @Data // Lombok注解，自动生成getter、setter等方法
    public static class ErrorDetails {
        /**
         * 错误发生的时间戳
         */
        private final LocalDateTime timestamp;

        /**
         * 错误消息
         */
        private final String message;

        /**
         * 错误详情，通常包含请求信息
         */
        private final String details;
    }

    // 可以添加更多的异常处理方法，例如：
    // @ExceptionHandler(DataIntegrityViolationException.class)
    // public ResponseEntity<?> handleDataIntegrityViolationException(...) { ... }

    // @ExceptionHandler(MethodArgumentNotValidException.class)
    // public ResponseEntity<?> handleValidationExceptions(...) { ... }
}
