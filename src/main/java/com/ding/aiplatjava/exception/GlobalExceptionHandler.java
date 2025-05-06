package com.ding.aiplatjava.exception;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

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
     * 处理无效凭证异常 (例如，登录时用户名或密码错误)
     *
     * @param ex 捕获到的 BadCredentialsException 异常
     * @param request 当前 Web 请求
     * @return 包含错误详情的 ResponseEntity，状态码 401
     */
    @ExceptionHandler(BadCredentialsException.class) // 专门处理 BadCredentialsException
    public ResponseEntity<?> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {

        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                "无效的凭证", // 可以自定义更友好的消息
                request.getDescription(false));

        // 返回 401 状态码
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 处理请求体验证失败异常 (例如 @Valid 注解触发)
     *
     * @param ex 捕获到的 MethodArgumentNotValidException 异常
     * @param request 当前 Web 请求
     * @return 包含验证错误详情的 ResponseEntity，状态码 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        // 从异常中提取所有字段的验证错误信息
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                "验证失败: " + errors, // 将具体的验证错误放入消息中
                request.getDescription(false));

        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理所有其他未明确处理的异常
     * 作为默认的异常处理器，捕获所有其他类型的异常
     *
     * @param ex 捕获到的Exception异常
     * @param request 当前Web请求
     * @return 包含错误详情的ResponseEntity，状态码500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(
            Exception ex, WebRequest request) {

        // !!! 重要: 检查是否是 ResponseStatusException，如果是，则优先使用它的状态码和原因 !!!
        if (ex instanceof ResponseStatusException rse) {
            ErrorDetails errorDetails = new ErrorDetails(
                    LocalDateTime.now(),
                    rse.getReason() != null ? rse.getReason() : "发生错误", // 使用 ResponseStatusException 的原因
                    request.getDescription(false));
            // 使用 ResponseStatusException 的状态码
            return new ResponseEntity<>(errorDetails, rse.getStatusCode());
        }

        // 对于其他所有未知异常，返回 500
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),
                ex.getMessage() != null ? ex.getMessage() : "发生内部错误",
                request.getDescription(false));

        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 错误详情类
     * 定义了API错误响应的标准格式
     */
    @Data
    @RequiredArgsConstructor
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
}
