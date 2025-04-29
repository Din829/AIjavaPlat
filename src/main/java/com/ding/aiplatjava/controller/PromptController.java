package com.ding.aiplatjava.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ding.aiplatjava.entity.Prompt;
import com.ding.aiplatjava.service.PromptService;

/**
 * Prompt 相关API控制器
 */
@RestController
@RequestMapping("/api/prompts") // 定义基础路径
public class PromptController {

    @Autowired
    private PromptService promptService;

    /**
     * 获取当前用户的所有Prompts
     * @return Prompt列表
     */
    @GetMapping
    public ResponseEntity<List<Prompt>> getCurrentUserPrompts(/* @AuthenticationPrincipal UserDetailsImpl currentUser */) {
        // TODO: 替换为从安全上下文获取用户ID
        Long currentUserId = 1L; // 临时硬编码，需要替换
        List<Prompt> prompts = promptService.getPromptsByUserId(currentUserId);
        return ResponseEntity.ok(prompts);
    }

    /**
     * 根据ID获取单个Prompt
     * @param id Prompt ID
     * @return Prompt实体或404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Prompt> getPromptById(@PathVariable Long id /*, @AuthenticationPrincipal UserDetailsImpl currentUser */) {
        // TODO: 替换为从安全上下文获取用户ID
        Long currentUserId = 1L; // 临时硬编码，需要替换
        Prompt prompt = promptService.getPromptById(id, currentUserId);
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
    public ResponseEntity<Prompt> createPrompt(@RequestBody Prompt prompt /*, @AuthenticationPrincipal UserDetailsImpl currentUser */) {
        // TODO: 替换为从安全上下文获取用户ID
        Long currentUserId = 1L; // 临时硬编码，需要替换

        // 通常请求体不包含 userId, createdAt, updatedAt，这些由后端设置
        Prompt createdPrompt = promptService.createPrompt(prompt, currentUserId);
        // 返回状态码 201 Created，并在 Location header 中提供新资源的URL (可选)
        // URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
        //         .buildAndExpand(createdPrompt.getId()).toUri();
        // return ResponseEntity.created(location).body(createdPrompt);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPrompt);
    }

    /**
     * 更新现有的Prompt
     * @param id 要更新的Prompt ID
     * @param promptDetails 请求体中的更新数据 (title, content, category)
     * @return 更新后的Prompt实体或404 Not Found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Prompt> updatePrompt(@PathVariable Long id, @RequestBody Prompt promptDetails /*, @AuthenticationPrincipal UserDetailsImpl currentUser */) {
        // TODO: 替换为从安全上下文获取用户ID
        Long currentUserId = 1L; // 临时硬编码，需要替换
        Prompt updatedPrompt = promptService.updatePrompt(id, promptDetails, currentUserId);
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
    public ResponseEntity<Void> deletePrompt(@PathVariable Long id /*, @AuthenticationPrincipal UserDetailsImpl currentUser */) {
        // TODO: 替换为从安全上下文获取用户ID
        Long currentUserId = 1L; // 临时硬编码，需要替换
        boolean deleted = promptService.deletePrompt(id, currentUserId);
        if (deleted) {
            return ResponseEntity.noContent().build(); // 204 No Content
        } else {
            return ResponseEntity.notFound().build();
        }
    }
} 