package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.ApiTokenDto;
import com.ding.aiplatjava.entity.ApiToken;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.service.ApiTokenService;
import com.ding.aiplatjava.service.UserService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ApiTokenController 的单元测试类。
 * 测试管理用户 API Token 的相关接口（获取列表、创建、删除）。
 * 使用 @WebMvcTest 专注于 Controller 层。
 * 使用 @WithMockUser 模拟已认证的用户，因为这些接口都需要认证。
 * 需要模拟 ApiTokenService 和 UserService (以及安全相关依赖)。
 */
@WebMvcTest(ApiTokenController.class) // 指定测试目标 Controller
class ApiTokenControllerTest {

    // 自动注入 MockMvc 用于模拟 HTTP 请求
    @Autowired
    private MockMvc mockMvc;

    // 模拟 ApiTokenService
    @MockBean
    private ApiTokenService apiTokenService;

    // 模拟 UserService (Controller 中的 getCurrentUser() 方法需要)
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
    // 存储测试用的 Token 实体对象
    private ApiToken testToken1;
    // 存储测试用的 Token DTO 对象 (用于响应对比)
    private ApiTokenDto testTokenDto1;

    /**
     * 在每个测试方法执行前运行的设置方法。
     * 用于初始化通用的测试数据和模拟行为。
     */
    @BeforeEach // JUnit 5 注解，标记在每个测试前执行
    void setUp() {
        // 创建模拟的测试用户对象
        testUser = new User();
        testUser.setId(1L); // 设置用户 ID
        testUser.setUsername("testuser"); // 设置用户名
        testUser.setEmail("test@example.com"); // 设置邮箱 (虽然此测试类不直接用)
        // **关键**: 配置模拟的 userService：当调用 findByUsername("testuser") 时，返回包含 testUser 的 Optional 对象。
        // 这是为了让 Controller 内部的 getCurrentUser() 方法能够成功获取到模拟用户。
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 创建模拟的 API Token 实体对象
        testToken1 = new ApiToken();
        testToken1.setId(101L); // 设置 Token ID
        testToken1.setUserId(testUser.getId()); // 关联用户 ID
        testToken1.setProvider("openai"); // 设置提供商
        testToken1.setTokenValue("encrypted-token-value"); // Service 层返回的实体可能包含加密后的值
        testToken1.setCreatedAt(LocalDateTime.now().minusDays(1)); // 设置创建时间
        testToken1.setUpdatedAt(LocalDateTime.now()); // 设置更新时间

        // 创建对应的 DTO 对象，用于对比 Controller 返回的响应体
        testTokenDto1 = new ApiTokenDto();
        testTokenDto1.setId(testToken1.getId());
        testTokenDto1.setUserId(testToken1.getUserId());
        testTokenDto1.setProvider(testToken1.getProvider());
        testTokenDto1.setTokenValue(null); // **重要**: Controller 返回的 DTO 不应包含 Token 值 (无论是明文还是加密)
        testTokenDto1.setCreatedAt(testToken1.getCreatedAt());
        testTokenDto1.setUpdatedAt(testToken1.getUpdatedAt());
    }

    /**
     * 测试获取当前用户 Token 列表 (GET /api/tokens) - 成功场景。
     * 预期: 返回 200 OK 和包含多个 Token DTO (不含 tokenValue) 的 JSON 数组。
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test // 标记为测试方法
    @WithMockUser(username = "testuser") // 使用 Spring Security Test 模拟一个用户名为 "testuser" 的已认证用户执行此测试
    void getCurrentUserTokens_Success() throws Exception {
        // Arrange: 准备阶段，设置模拟对象的行为
        ApiToken testToken2 = new ApiToken(); // 创建第二个 Token 实体用于测试列表返回
        testToken2.setId(102L);
        testToken2.setUserId(testUser.getId());
        testToken2.setProvider("google-ai");
        // 配置模拟的 apiTokenService：当调用 getTokensByUserId (使用 testUser 的 ID) 时，返回包含两个 Token 实体的列表
        when(apiTokenService.getTokensByUserId(testUser.getId())).thenReturn(Arrays.asList(testToken1, testToken2));

        // Act & Assert: 执行请求并进行断言
        mockMvc.perform(get("/api/tokens")) // 模拟发送 GET 请求到 /api/tokens
                .andExpect(status().isOk()) // 断言：期望状态码为 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 断言：期望响应内容类型为 application/json
                .andExpect(jsonPath("$.length()").value(2)) // 断言：期望返回的 JSON 数组长度为 2
                .andExpect(jsonPath("$[0].id").value(testToken1.getId())) // 断言：数组第一个元素的 id 正确
                .andExpect(jsonPath("$[0].provider").value(testToken1.getProvider())) // 断言：数组第一个元素的 provider 正确
                .andExpect(jsonPath("$[0].tokenValue").doesNotExist()) // **关键断言**: 确认响应中不包含 tokenValue 字段
                .andExpect(jsonPath("$[1].id").value(testToken2.getId())) // 断言：数组第二个元素的 id 正确
                .andExpect(jsonPath("$[1].provider").value(testToken2.getProvider())); // 断言：数组第二个元素的 provider 正确
    }

    /**
     * 测试获取当前用户 Token 列表 (GET /api/tokens) - 用户无 Token 场景。
     * 预期: 返回 200 OK 和一个空的 JSON 数组。
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void getCurrentUserTokens_NoTokens() throws Exception {
        // Arrange: 配置模拟的 apiTokenService 返回一个空列表
        when(apiTokenService.getTokensByUserId(testUser.getId())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/tokens")) // 模拟 GET 请求
                .andExpect(status().isOk()) // 断言：期望状态码 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 断言：期望内容类型为 JSON
                .andExpect(jsonPath("$.length()").value(0)); // 断言：期望返回的 JSON 数组长度为 0
    }

    /**
     * 测试创建 Token (POST /api/tokens) - 成功场景。
     * 预期: 返回 201 Created 状态码和创建后的 Token DTO (不含 tokenValue)。
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void createToken_Success() throws Exception {
        // Arrange: 准备用于请求体的 DTO，只包含用户需要提供的信息
        ApiTokenDto requestDto = new ApiTokenDto();
        requestDto.setProvider("new-provider"); // 设置提供商
        requestDto.setTokenValue("plain-text-token"); // 设置明文 Token 值 (Service 层会负责加密)

        // 准备模拟 Service 层成功创建后返回的实体对象
        ApiToken createdToken = new ApiToken();
        createdToken.setId(103L); // 假设数据库生成了 ID
        createdToken.setUserId(testUser.getId()); // Service 层会设置用户 ID
        createdToken.setProvider(requestDto.getProvider());
        createdToken.setTokenValue("encrypted-new-token"); // Service 层返回加密后的值
        createdToken.setCreatedAt(LocalDateTime.now());
        createdToken.setUpdatedAt(LocalDateTime.now());

        // 配置模拟的 apiTokenService：当调用 createToken 方法 (匹配任何 ApiToken 对象和 testUser 的 ID) 时，返回上面准备的 createdToken 实体
        // 使用 any(ApiToken.class) 匹配第一个参数，因为 Controller 内部会 new 一个 ApiToken 对象传入，内容不易精确匹配
        // 使用 eq(testUser.getId()) 精确匹配用户 ID
        when(apiTokenService.createToken(any(ApiToken.class), eq(testUser.getId()))).thenReturn(createdToken);

        // Act & Assert
        mockMvc.perform(post("/api/tokens") // 模拟 POST 请求
                        .with(csrf()) // **重要**: 添加 CSRF token，因为 @WebMvcTest 默认启用 Security，POST 需要 CSRF 保护
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求类型
                        .content(objectMapper.writeValueAsString(requestDto))) // 将请求 DTO 序列化为 JSON 请求体
                .andExpect(status().isCreated()) // 断言：期望状态码为 201 Created
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 断言：期望内容类型为 JSON
                .andExpect(jsonPath("$.id").value(createdToken.getId())) // 断言：响应体中 id 正确
                .andExpect(jsonPath("$.provider").value(createdToken.getProvider())) // 断言：响应体中 provider 正确
                .andExpect(jsonPath("$.tokenValue").doesNotExist()); // **关键断言**: 确认响应 DTO 中不包含 tokenValue
    }

    /**
     * 测试删除 Token (DELETE /api/tokens/{id}) - 成功场景。
     * 预期: 返回 204 No Content 状态码。
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void deleteToken_Success() throws Exception {
        // Arrange: 准备要删除的 Token ID
        Long tokenIdToDelete = testToken1.getId();
        // 配置模拟的 apiTokenService：当调用 deleteToken (匹配 ID 和用户 ID) 时，返回 true (表示删除成功)
        when(apiTokenService.deleteToken(eq(tokenIdToDelete), eq(testUser.getId()))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/tokens/{id}", tokenIdToDelete) // 模拟 DELETE 请求，并传入路径变量 id
                        .with(csrf())) // **重要**: 添加 CSRF token
                .andExpect(status().isNoContent()); // 断言：期望状态码为 204 No Content
    }

    /**
     * 测试删除 Token (DELETE /api/tokens/{id}) - 失败场景 (Token 不存在或不属于该用户)。
     * 预期: 返回 404 Not Found 状态码。
     * @throws Exception 测试过程中可能抛出的异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void deleteToken_NotFoundOrNotAuthorized() throws Exception {
        // Arrange: 准备一个不存在的 Token ID
        Long nonExistentTokenId = 999L;
        // 配置模拟的 apiTokenService：当调用 deleteToken (匹配 ID 和用户 ID) 时，返回 false (表示删除失败，例如 Token 不存在或不属于该用户)
        when(apiTokenService.deleteToken(eq(nonExistentTokenId), eq(testUser.getId()))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/tokens/{id}", nonExistentTokenId) // 模拟 DELETE 请求
                        .with(csrf())) // **重要**: 添加 CSRF token
                .andExpect(status().isNotFound()); // 断言：期望状态码为 404 Not Found
    }
} 