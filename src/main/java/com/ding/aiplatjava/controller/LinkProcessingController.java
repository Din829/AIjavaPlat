package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.LinkProcessRequestDto;
import com.ding.aiplatjava.dto.LinkProcessResponseDto;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.service.LinkProcessingService;
import com.ding.aiplatjava.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 链接处理控制器
 * 提供链接处理相关的REST API端点，支持网页摘要和视频转写
 */
@Slf4j
@RestController
@RequestMapping("/api/link-processing")
@RequiredArgsConstructor
public class LinkProcessingController {

    private final LinkProcessingService linkProcessingService;
    private final UserService userService;

    /**
     * 处理链接（网页或视频）
     * 自动识别链接类型并进行相应处理
     * 
     * @param requestDto 链接处理请求DTO
     * @return 处理响应，包含任务ID和初始状态
     */
    @PostMapping("/process")
    public ResponseEntity<LinkProcessResponseDto> processLink(
            @Valid @RequestBody LinkProcessRequestDto requestDto) {
        
        try {
            User currentUser = getCurrentUser();
            log.info("收到链接处理请求: {}, 用户: {}", requestDto.getUrl(), currentUser.getUsername());
            
            LinkProcessResponseDto response = linkProcessingService.processLink(requestDto, currentUser.getId());
            
            log.info("链接处理任务创建成功: {}", response.getTaskId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("处理链接时发生错误: {}", requestDto.getUrl(), e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "处理链接时发生错误: " + e.getMessage()
            );
        }
    }

    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态和基本信息
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<LinkProcessResponseDto> getTaskStatus(@PathVariable String taskId) {
        try {
            User currentUser = getCurrentUser();
            log.debug("获取任务状态请求: {}, 用户: {}", taskId, currentUser.getUsername());
            
            LinkProcessResponseDto response = linkProcessingService.getTaskStatus(taskId, currentUser.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取任务状态时发生错误: {}", taskId, e);
            
            if (e.getMessage().contains("不存在或无权访问")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在或无权访问");
            }
            
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "获取任务状态失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取任务结果
     * 
     * @param taskId 任务ID
     * @return 完整的任务结果
     */
    @GetMapping("/result/{taskId}")
    public ResponseEntity<LinkProcessResponseDto> getTaskResult(@PathVariable String taskId) {
        try {
            User currentUser = getCurrentUser();
            log.debug("获取任务结果请求: {}, 用户: {}", taskId, currentUser.getUsername());
            
            LinkProcessResponseDto response = linkProcessingService.getTaskResult(taskId, currentUser.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取任务结果时发生错误: {}", taskId, e);
            
            if (e.getMessage().contains("不存在或无权访问")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在或无权访问");
            }
            
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "获取任务结果失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取用户的所有任务
     * 
     * @return 用户的任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<LinkProcessResponseDto>> getUserTasks() {
        try {
            User currentUser = getCurrentUser();
            log.debug("获取用户任务列表请求, 用户: {}", currentUser.getUsername());
            
            List<LinkProcessResponseDto> tasks = linkProcessingService.getUserTasks(currentUser.getId());
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            log.error("获取用户任务列表时发生错误", e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "获取任务列表失败: " + e.getMessage()
            );
        }
    }

    /**
     * 删除用户的任务
     * 
     * @param taskId 任务ID
     * @return 删除结果
     */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable String taskId) {
        try {
            User currentUser = getCurrentUser();
            log.info("删除任务请求: {}, 用户: {}", taskId, currentUser.getUsername());
            
            boolean success = linkProcessingService.deleteTask(taskId, currentUser.getId());
            
            if (success) {
                log.info("任务删除成功: {}", taskId);
                return ResponseEntity.noContent().build();
            } else {
                log.warn("任务删除失败，任务不存在或无权访问: {}", taskId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在或无权访问");
            }
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除任务时发生错误: {}", taskId, e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "删除任务失败: " + e.getMessage()
            );
        }
    }

    /**
     * 获取当前认证用户
     * 
     * @return 当前用户对象
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        User user = userService.findByUsername(username);
        if (user == null) {
            log.error("无法找到当前用户: {}", username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户认证失败");
        }
        
        return user;
    }
}
