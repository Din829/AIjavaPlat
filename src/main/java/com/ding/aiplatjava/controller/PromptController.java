package com.ding.aiplatjava.controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.ding.aiplatjava.entity.Prompt;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.service.PromptService;
import com.ding.aiplatjava.service.UserService;
import com.ding.aiplatjava.dto.PromptDto;

/**
 * Prompt 相关API控制器
 */
@RestController
@RequestMapping("/api/prompts") // 定义基础路径
public class PromptController {

    @Autowired
    private PromptService promptService;

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户的 User 实体。
     * 如果用户未认证或在数据库中找不到，则抛出异常。
     *
     * @return 当前登录用户的 User 对象。
     * @throws ResponseStatusException 如果用户未认证或找不到。
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof UserDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in database"));
    }

    /**
     * 获取当前用户的所有Prompts
     * @return Prompt DTO 列表
     */
    @GetMapping
    public ResponseEntity<List<PromptDto>> getCurrentUserPrompts() {
        User currentUser = getCurrentUser();
        List<Prompt> prompts = promptService.getPromptsByUserId(currentUser.getId());

        // ---- 临时调试日志 ----
        System.out.println("--- Debug: Fetched Prompts from Service ---");
        prompts.forEach(p -> {
            System.out.println("Prompt ID: " + p.getId() + ", CreatedAt: " + p.getCreatedAt() + ", UpdatedAt: " + p.getUpdatedAt());
        });
        System.out.println("--- End Debug ---");
        // ---- 结束调试日志 ----

        // 将 List<Prompt> 转换为 List<PromptDto>
        List<PromptDto> promptDtos = prompts.stream()
                                            .map(this::convertToDto) // 使用转换方法
                                            .collect(Collectors.toList());
        return ResponseEntity.ok(promptDtos);
    }

    /**
     * 根据ID获取单个Prompt
     * @param id Prompt ID
     * @return Prompt实体或404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Prompt> getPromptById(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Prompt prompt = promptService.getPromptById(id, currentUser.getId());
        if (prompt != null) {
            return ResponseEntity.ok(prompt);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 创建新的Prompt
     * @param prompt 请求体中的Prompt数据 (title, content, category)
     * @return 创建后的Prompt实体和201 Created状态
     */
    @PostMapping
    public ResponseEntity<Prompt> createPrompt(@RequestBody Prompt prompt) {
        User currentUser = getCurrentUser();
        Prompt createdPrompt = promptService.createPrompt(prompt, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPrompt);
    }

    /**
     * 更新现有的Prompt
     * @param id 要更新的Prompt ID
     * @param promptDetails 请求体中的更新数据 (title, content, category)
     * @return 更新后的Prompt实体或404 Not Found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Prompt> updatePrompt(@PathVariable Long id, @RequestBody Prompt promptDetails) {
        User currentUser = getCurrentUser();
        Prompt updatedPrompt = promptService.updatePrompt(id, promptDetails, currentUser.getId());
        if (updatedPrompt != null) {
            return ResponseEntity.ok(updatedPrompt);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除Prompt
     * @param id 要删除的Prompt ID
     * @return 204 No Content 或 404 Not Found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        boolean deleted = promptService.deletePrompt(id, currentUser.getId());
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private PromptDto convertToDto(Prompt prompt) {
        if (prompt == null) {
            return null;
        }
        PromptDto dto = new PromptDto();
        dto.setId(prompt.getId());
        dto.setTitle(prompt.getTitle());
        dto.setContent(prompt.getContent());
        dto.setCategory(prompt.getCategory());
        dto.setCreatedAt(prompt.getCreatedAt());
        dto.setUpdatedAt(prompt.getUpdatedAt());
        return dto;
    }
} 