package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.OcrResponseDto;
import com.ding.aiplatjava.dto.OcrTaskStatusDto;
import com.ding.aiplatjava.dto.OcrUploadRequestDto;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.exception.ResourceNotFoundException;
import com.ding.aiplatjava.service.OcrService;
import com.ding.aiplatjava.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * OCR控制器
 * 处理OCR相关的HTTP请求
 */
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private static final Logger log = LoggerFactory.getLogger(OcrController.class);
    
    private final OcrService ocrService;
    private final UserService userService;
    
    /**
     * 获取当前登录用户
     * 
     * @return 当前登录用户
     * @throws ResponseStatusException 如果用户未认证或找不到
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未认证");
        }
        
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "在数据库中找不到用户"));
    }
    
    /**
     * 上传文件进行OCR处理
     * 
     * @param file 上传的文件
     * @param requestDto OCR请求参数
     * @return OCR响应DTO
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OcrResponseDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @ModelAttribute OcrUploadRequestDto requestDto) {
        
        log.info("收到OCR文件上传请求，文件名: {}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            log.warn("上传的文件为空");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要上传的文件");
        }
        
        try {
            // 获取当前用户
            User currentUser = getCurrentUser();
            log.debug("当前用户: {}", currentUser.getUsername());
            
            // 调用服务处理上传
            OcrResponseDto response = ocrService.uploadAndProcess(file, currentUser.getId(), requestDto);
            
            log.info("文件上传处理成功，任务ID: {}", response.getTaskId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("处理OCR文件上传请求时发生错误", e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "处理文件时发生错误: " + e.getMessage()
            );
        }
    }
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态DTO
     */
    @GetMapping("/tasks/{taskId}/status")
    public ResponseEntity<OcrTaskStatusDto> getTaskStatus(@PathVariable String taskId) {
        log.debug("收到获取任务状态请求，任务ID: {}", taskId);
        
        try {
            // 获取当前用户
            User currentUser = getCurrentUser();
            
            // 获取任务状态
            OcrTaskStatusDto statusDto = ocrService.getTaskStatus(taskId, currentUser.getId());
            
            return ResponseEntity.ok(statusDto);
            
        } catch (ResourceNotFoundException e) {
            log.warn("任务未找到，任务ID: {}", taskId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("获取任务状态时发生错误", e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "获取任务状态时发生错误: " + e.getMessage()
            );
        }
    }
    
    /**
     * 获取任务结果
     * 
     * @param taskId 任务ID
     * @return OCR响应DTO
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<OcrResponseDto> getTaskResult(@PathVariable String taskId) {
        log.debug("收到获取任务结果请求，任务ID: {}", taskId);
        
        try {
            // 获取当前用户
            User currentUser = getCurrentUser();
            
            // 获取任务结果
            OcrResponseDto responseDto = ocrService.getTaskResult(taskId, currentUser.getId());
            
            return ResponseEntity.ok(responseDto);
            
        } catch (ResourceNotFoundException e) {
            log.warn("任务未找到，任务ID: {}", taskId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("获取任务结果时发生错误", e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "获取任务结果时发生错误: " + e.getMessage()
            );
        }
    }
    
    /**
     * 获取用户的所有任务
     * 
     * @return 任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<OcrResponseDto>> getUserTasks() {
        log.debug("收到获取用户任务列表请求");
        
        try {
            // 获取当前用户
            User currentUser = getCurrentUser();
            
            // 获取用户任务列表
            List<OcrResponseDto> tasks = ocrService.getUserTasks(currentUser.getId());
            
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            log.error("获取用户任务列表时发生错误", e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "获取任务列表时发生错误: " + e.getMessage()
            );
        }
    }
}
