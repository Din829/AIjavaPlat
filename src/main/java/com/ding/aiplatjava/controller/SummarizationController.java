package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.SummarizationRequestDto;
import com.ding.aiplatjava.dto.SummarizationResponseDto;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.exception.ResourceNotFoundException;
import com.ding.aiplatjava.service.AiService;
import com.ding.aiplatjava.service.ApiTokenService;
import com.ding.aiplatjava.service.UserService;
import com.ding.aiplatjava.service.WebContentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * 处理网页内容摘要请求的 REST 控制器。
 */
@RestController
@RequestMapping("/api/summarize")
@RequiredArgsConstructor
public class SummarizationController {

    private static final Logger log = LoggerFactory.getLogger(SummarizationController.class);

    private final WebContentService webContentService;
    private final AiService aiService;
    private final ApiTokenService apiTokenService;
    private final UserService userService;

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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未认证");
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "在数据库中找不到用户"));
    }

    /**
     * 处理网页摘要请求。
     *
     * @param requestDto 包含目标 URL 的请求体。
     * @return 包含摘要结果的响应实体，或在出错时返回错误状态码。
     */
    @PostMapping
    public ResponseEntity<SummarizationResponseDto> summarizeUrl(@Valid @RequestBody SummarizationRequestDto requestDto) {
        log.info("收到URL摘要请求: {}", requestDto.getUrl());

        // 1. 获取当前认证用户
        User currentUser = getCurrentUser();
        Long currentUserId = currentUser.getId();
        log.debug("已认证用户ID: {}", currentUserId);

        try {
            // 2. 获取用户的OpenAI API密钥
            String provider = "openai"; // 指定提供商为OpenAI
            log.debug("正在获取用户ID: {} 的 {} API密钥", currentUserId, provider);
            String apiKey = apiTokenService.getDecryptedTokenValueByProvider(currentUserId, provider);

            if (apiKey == null || apiKey.isEmpty()) {
                log.warn("用户ID: {} 的 {} API密钥为空，将使用全局配置", currentUserId, provider);
                throw new ResourceNotFoundException("未找到API密钥，请先添加您的OpenAI密钥");
            }

            log.info("成功获取用户的API密钥");

            // 3. 提取网页文本
            log.debug("正在从URL提取文本: {}", requestDto.getUrl());
            String webText = webContentService.extractTextFromUrl(requestDto.getUrl());
            if (webText.isEmpty()) {
                log.warn("URL提取的文本为空: {}", requestDto.getUrl());
                return ResponseEntity.ok(new SummarizationResponseDto("无法提取网页内容或内容为空。"));
            }
            log.debug("文本提取成功。长度: {}", webText.length());

            // 4. 调用 AI 服务进行摘要，传入用户的API密钥
            log.debug("正在调用AI服务进行摘要...");
            String summary = aiService.summarizeText(webText, apiKey);
            log.info("URL摘要生成成功: {}", requestDto.getUrl());

            // 5. 构建并返回响应
            SummarizationResponseDto responseDto = new SummarizationResponseDto(summary);
            return ResponseEntity.ok(responseDto);

        } catch (ResourceNotFoundException e) {
            log.error("未找到用户ID: {} 的'openai'提供商API令牌", currentUserId, e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到API密钥，请先添加您的OpenAI密钥", e);
        } catch (IOException e) {
            log.error("提取URL内容时出错: {}", requestDto.getUrl(), e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法访问或处理URL: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("AI摘要过程中出错，URL: {}", requestDto.getUrl(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI服务调用失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("摘要处理过程中发生意外错误，URL: {}", requestDto.getUrl(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "处理摘要请求时发生意外错误", e);
        }
    }
}