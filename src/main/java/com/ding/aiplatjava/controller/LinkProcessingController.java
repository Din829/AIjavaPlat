package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.LinkProcessRequestDto;
import com.ding.aiplatjava.dto.LinkProcessResponseDto;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.service.LinkProcessingService;
import com.ding.aiplatjava.service.UserService;
import com.ding.aiplatjava.service.LinkAnalysisService;
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
import java.util.Map;
import java.util.HashMap;

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
    private final LinkAnalysisService linkAnalysisService;

    /**
     * 分析链接类型和支持情况
     * 用于前端在用户输入URL后立即获取链接分析结果
     * 
     * @param requestBody 包含url字段的请求体
     * @return 链接分析结果
     */
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeLink(@RequestBody Map<String, String> requestBody) {
        try {
            String url = requestBody.get("url");
            if (url == null || url.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL不能为空");
            }

            User currentUser = getCurrentUser();
            log.info("收到链接分析请求: {}, 用户: {}", url, currentUser.getUsername());

            // 1. 检测链接类型
            String contentType = linkAnalysisService.detectContentType(url);
            
            // 2. 检查URL可访问性
            boolean isAccessible = linkAnalysisService.isUrlAccessible(url);
            
            // 3. 提取标题/元数据
            String title = "";
            String description = "";
            String platform = "";
            
            if ("VIDEO".equals(contentType)) {
                Map<String, Object> metadata = linkAnalysisService.extractVideoMetadata(url);
                title = (String) metadata.getOrDefault("title", "待提取");
                description = (String) metadata.getOrDefault("description", "");
                platform = (String) metadata.getOrDefault("platform", "Unknown");
            } else {
                title = linkAnalysisService.extractWebPageTitle(url);
                platform = "Web";
            }

            // 4. 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("linkType", contentType);
            response.put("platform", platform);
            response.put("title", title);
            response.put("description", description);
            response.put("isSupported", isAccessible);
            response.put("message", 
                contentType.equals("VIDEO") ? 
                    (isAccessible ? "支持的视频链接，可以进行转写处理" : "视频链接暂时无法访问") :
                    (isAccessible ? "支持的网页链接，可以进行摘要处理" : "网页链接暂时无法访问")
            );

            log.info("链接分析完成: {} -> {}", url, contentType);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("分析链接时发生错误: {}", requestBody.get("url"), e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "分析链接时发生错误: " + e.getMessage()
            );
        }
    }

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
     * 获取任务详情（前端兼容性端点）
     * 为了与前端的API调用保持一致
     * 
     * @param taskId 任务ID
     * @return 完整的任务详情和结果
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<LinkProcessResponseDto> getTaskDetail(@PathVariable String taskId) {
        try {
            User currentUser = getCurrentUser();
            log.debug("获取任务详情请求: {}, 用户: {}", taskId, currentUser.getUsername());
            
            // 优先返回完整结果，如果任务未完成则返回状态
            LinkProcessResponseDto response = linkProcessingService.getTaskResult(taskId, currentUser.getId());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取任务详情时发生错误: {}", taskId, e);
            
            if (e.getMessage().contains("不存在或无权访问")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在或无权访问");
            }
            
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "获取任务详情失败: " + e.getMessage()
            );
        }
    }

    /**
     * 删除指定任务（前端兼容性端点）
     * 为了与前端的API调用保持一致
     * 
     * @param taskId 任务ID
     * @return 删除结果
     */
    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<Void> deleteTaskById(@PathVariable String taskId) {
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
     * 检查链接处理相关微服务的健康状态
     *
     * @return 各微服务的健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkServiceHealth() {
        try {
            User currentUser = getCurrentUser(); // 确保是认证用户访问
            log.debug("服务健康检查请求, 用户: {}", currentUser.getUsername());
            Map<String, Object> healthStatus = linkProcessingService.checkServiceHealth();
            return ResponseEntity.ok(healthStatus);
        } catch (Exception e) {
            log.error("检查服务健康状态时发生错误", e);
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "检查服务健康状态失败: " + e.getMessage()
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
        
        return userService.findByUsername(username)
            .orElseThrow(() -> {
                log.error("无法找到当前用户: {}", username);
                return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户认证失败");
            });
    }
}
