package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.SummarizationRequestDto;
import com.ding.aiplatjava.dto.SummarizationResponseDto;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.exception.ResourceNotFoundException;
import com.ding.aiplatjava.service.AiService;
import com.ding.aiplatjava.service.ApiTokenService;
import com.ding.aiplatjava.service.UserService;
import com.ding.aiplatjava.service.WebContentService;
import com.ding.aiplatjava.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SummarizationController 的单元测试类。
 * 测试网页摘要接口 (/api/summarize) 的各种场景，包括成功、输入验证失败、依赖服务失败等。
 * 使用 @WebMvcTest 专注于 Controller 层。
 * 使用 @WithMockUser 模拟已认证的用户，因为此接口需要认证。
 * 需要模拟 WebContentService, AiService, ApiTokenService, UserService (以及安全相关依赖)。
 */
@WebMvcTest(SummarizationController.class) // 指定测试目标 Controller
class SummarizationControllerTest {

    // 自动注入 MockMvc 用于模拟 HTTP 请求
    @Autowired
    private MockMvc mockMvc;

    // 模拟网页内容服务
    @MockBean
    private WebContentService webContentService;

    // 模拟 AI 服务
    @MockBean
    private AiService aiService;

    // 模拟 API Token 服务
    @MockBean
    private ApiTokenService apiTokenService;

    // 模拟用户服务 (Controller 中的 getCurrentUser() 方法需要)
    @MockBean
    private UserService userService;

    // 添加模拟 Bean 以确保 Spring Security 相关配置在测试上下文中能加载成功
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private AuthenticationManager authenticationManager;

    // 自动注入 ObjectMapper 用于 JSON 序列化/反序列化
    @Autowired
    private ObjectMapper objectMapper;

    // 存储测试用户对象
    private User testUser;
    // 定义测试用的 URL
    private final String testUrl = "https://example.com/article";
    // 定义测试用的 API Key
    private final String testApiKey = "test-api-key";
    // 定义测试用的网页文本内容
    private final String testWebText = "This is the content of the web page.";
    // 定义测试用的 AI 摘要结果
    private final String testSummary = "This is the summary.";

    /**
     * 在每个测试方法执行前运行的设置方法。
     * 用于初始化通用的测试用户和模拟 userService 的行为。
     */
    @BeforeEach // JUnit 5 注解
    void setUp() {
        // 创建模拟的测试用户
        testUser = new User();
        testUser.setId(1L); // 设置用户 ID
        testUser.setUsername("testuser"); // 设置用户名
        // 配置模拟的 userService 以便 Controller 能获取到当前用户
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    /**
     * 测试摘要接口 (/api/summarize) - 成功场景。
     * 预期: 返回 200 OK 和包含摘要结果的 JSON 响应体。
     * @throws Exception 测试异常
     */
    @Test // 标记为测试方法
    @WithMockUser(username = "testuser") // 模拟认证用户
    void summarizeUrl_Success() throws Exception {
        // Arrange: 准备阶段，设置请求 DTO 和模拟依赖服务的行为
        SummarizationRequestDto requestDto = new SummarizationRequestDto(testUrl); // 创建请求 DTO
        // 模拟 ApiTokenService：成功获取到 API Key
        when(apiTokenService.getDecryptedTokenValueByProvider(eq(testUser.getId()), eq("openai"))).thenReturn(testApiKey);
        // 模拟 WebContentService：成功提取到网页文本
        when(webContentService.extractTextFromUrl(eq(testUrl))).thenReturn(testWebText);
        // 模拟 AiService：成功生成摘要
        when(aiService.summarizeText(eq(testWebText), eq(testApiKey))).thenReturn(testSummary);

        // Act & Assert: 执行请求并进行断言
        mockMvc.perform(post("/api/summarize") // 模拟 POST 请求
                        .with(csrf()) // 添加 CSRF token
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求类型
                        .content(objectMapper.writeValueAsString(requestDto))) // 设置请求体
                .andExpect(status().isOk()) // 断言：期望状态码 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 断言：期望内容类型 JSON
                .andExpect(jsonPath("$.originalUrl", is(testUrl))) // 断言：响应体 originalUrl 正确
                .andExpect(jsonPath("$.summary", is(testSummary))); // 断言：响应体 summary 正确
    }

    /**
     * 测试摘要接口 (/api/summarize) - 失败场景：URL 格式无效。
     * 预期: 返回 400 Bad Request (由 @Valid 注解和 GlobalExceptionHandler 处理)。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void summarizeUrl_ValidationFailure_InvalidUrl() throws Exception {
        // Arrange: 准备包含无效 URL 的请求 DTO
        SummarizationRequestDto requestDto = new SummarizationRequestDto("invalid-url");

        // Act & Assert: 执行请求并断言
        // 由于 SummarizationRequestDto 中的 url 字段上有 @URL 校验注解，Spring 会自动校验
        mockMvc.perform(post("/api/summarize") // 模拟 POST 请求
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()); // 断言：期望状态码 400 Bad Request (由参数校验失败触发)
    }

    /**
     * 测试摘要接口 (/api/summarize) - 失败场景：URL 为空。
     * 预期: 返回 400 Bad Request (由 @NotBlank 注解和 GlobalExceptionHandler 处理)。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void summarizeUrl_ValidationFailure_BlankUrl() throws Exception {
        // Arrange: 准备包含空 URL 的请求 DTO
        SummarizationRequestDto requestDto = new SummarizationRequestDto("");

        // Act & Assert: 执行请求并断言
        // 由于 SummarizationRequestDto 中的 url 字段上有 @NotBlank 校验注解
        mockMvc.perform(post("/api/summarize") // 模拟 POST 请求
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()); // 断言：期望状态码 400 Bad Request
    }

    /**
     * 测试摘要接口 (/api/summarize) - 失败场景：未找到用户对应的 API Key。
     * 预期: 返回 400 Bad Request (Controller 内部捕获 ResourceNotFoundException 并转换为 400)。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void summarizeUrl_ApiKeyNotFound() throws Exception {
        // Arrange: 准备请求 DTO
        SummarizationRequestDto requestDto = new SummarizationRequestDto(testUrl);
        // 模拟 ApiTokenService：抛出 ResourceNotFoundException，表示未找到 Key
        when(apiTokenService.getDecryptedTokenValueByProvider(eq(testUser.getId()), eq("openai")))
                .thenThrow(new ResourceNotFoundException("未找到API密钥"));

        // Act & Assert: 执行请求并断言
        mockMvc.perform(post("/api/summarize") // 模拟 POST 请求
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()); // 断言：期望状态码 400 Bad Request (Controller 内部处理了 ResourceNotFoundException)
    }

    /**
     * 测试摘要接口 (/api/summarize) - 失败场景：提取网页内容失败。
     * 预期: 返回 503 Service Unavailable (Controller 内部捕获 IOException 并转换为 503)。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void summarizeUrl_WebContentExtractionFailed() throws Exception {
        // Arrange: 准备请求 DTO
        SummarizationRequestDto requestDto = new SummarizationRequestDto(testUrl);
        // 模拟 ApiTokenService：成功返回 Key
        when(apiTokenService.getDecryptedTokenValueByProvider(eq(testUser.getId()), eq("openai"))).thenReturn(testApiKey);
        // 模拟 WebContentService：在提取内容时抛出 IOException
        when(webContentService.extractTextFromUrl(eq(testUrl))).thenThrow(new IOException("网络错误或无法访问URL"));

        // Act & Assert: 执行请求并断言
        mockMvc.perform(post("/api/summarize") // 模拟 POST 请求
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isServiceUnavailable()); // 断言：期望状态码 503 Service Unavailable (Controller 处理了 IOException)
    }

    /**
     * 测试摘要接口 (/api/summarize) - 失败场景：AI 服务调用失败。
     * 预期: 返回 500 Internal Server Error (Controller 内部捕获 RuntimeException 并转换为 500)。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void summarizeUrl_AiServiceFailed() throws Exception {
        // Arrange: 准备请求 DTO
        SummarizationRequestDto requestDto = new SummarizationRequestDto(testUrl);
        // 模拟 ApiTokenService：成功返回 Key
        when(apiTokenService.getDecryptedTokenValueByProvider(eq(testUser.getId()), eq("openai"))).thenReturn(testApiKey);
        // 模拟 WebContentService：成功返回网页内容
        when(webContentService.extractTextFromUrl(eq(testUrl))).thenReturn(testWebText);
        // 模拟 AiService：在摘要时抛出 RuntimeException
        when(aiService.summarizeText(eq(testWebText), eq(testApiKey))).thenThrow(new RuntimeException("AI 服务内部错误"));

        // Act & Assert: 执行请求并断言
        mockMvc.perform(post("/api/summarize") // 模拟 POST 请求
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isInternalServerError()); // 断言：期望状态码 500 Internal Server Error (Controller 处理了 RuntimeException)
    }

    /**
     * 测试摘要接口 (/api/summarize) - 边界场景：提取到的网页内容为空。
     * 预期: 返回 400 Bad Request (Controller 检查到内容为空并返回 400)。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void summarizeUrl_EmptyWebContent() throws Exception {
        // Arrange: 准备请求 DTO
        SummarizationRequestDto requestDto = new SummarizationRequestDto(testUrl);
        // 模拟 ApiTokenService：成功返回 Key
        when(apiTokenService.getDecryptedTokenValueByProvider(eq(testUser.getId()), eq("openai"))).thenReturn(testApiKey);
        // 模拟 WebContentService：返回空字符串
        when(webContentService.extractTextFromUrl(eq(testUrl))).thenReturn("");

        // Act & Assert: 执行请求并断言
        mockMvc.perform(post("/api/summarize") // 模拟 POST 请求
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest()); // 断言：期望状态码 400 Bad Request (Controller 检查到空内容)
    }
} 