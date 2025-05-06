package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.PromptDto;
import com.ding.aiplatjava.entity.Prompt;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.service.PromptService;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PromptController 的单元测试类。
 * 测试管理用户 Prompt 的 CRUD (创建、读取、更新、删除) 相关接口。
 * 使用 @WebMvcTest 专注于 Controller 层。
 * 使用 @WithMockUser 模拟已认证的用户，因为所有接口都需要认证。
 * 需要模拟 PromptService 和 UserService (以及安全相关依赖)。
 */
@WebMvcTest(PromptController.class) // 指定测试目标 Controller
class PromptControllerTest {

    // 自动注入 MockMvc 用于模拟 HTTP 请求
    @Autowired
    private MockMvc mockMvc;

    // 模拟 PromptService
    @MockBean
    private PromptService promptService;

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
    // 存储测试用的 Prompt 实体对象 1
    private Prompt testPrompt1;
    // 存储测试用的 Prompt 实体对象 2
    private Prompt testPrompt2;

    /**
     * 在每个测试方法执行前运行的设置方法。
     * 用于初始化通用的测试数据和模拟行为。
     */
    @BeforeEach // JUnit 5 注解
    void setUp() {
        // 创建模拟的测试用户
        testUser = new User();
        testUser.setId(1L); // 设置用户 ID
        testUser.setUsername("testuser"); // 设置用户名
        // 配置模拟的 userService 以便 Controller 能获取到当前用户
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // 创建模拟的 Prompt 实体对象
        testPrompt1 = new Prompt(101L, testUser.getId(), "Test Prompt 1", "Content 1", "Test", LocalDateTime.now().minusDays(1), LocalDateTime.now());
        testPrompt2 = new Prompt(102L, testUser.getId(), "Test Prompt 2", "Content 2", "Test", LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 测试获取当前用户的所有 Prompts (GET /api/prompts) - 成功场景。
     * 预期: 返回 200 OK 和包含多个 Prompt DTO 的 JSON 数组。
     * 注意: Controller 返回的是 DTO 列表，即使 Service 返回的是实体列表。
     * @throws Exception 测试异常
     */
    @Test // 标记为测试方法
    @WithMockUser(username = "testuser") // 模拟认证用户
    void getCurrentUserPrompts_Success() throws Exception {
        // Arrange: 配置模拟 promptService 返回 Prompt 实体列表
        when(promptService.getPromptsByUserId(testUser.getId())).thenReturn(Arrays.asList(testPrompt1, testPrompt2));

        // Act & Assert: 执行请求并进行断言
        mockMvc.perform(get("/api/prompts")) // 模拟 GET 请求
                .andExpect(status().isOk()) // 断言：期望状态码 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 断言：期望内容类型 JSON
                .andExpect(jsonPath("$", hasSize(2))) // 断言：使用 Hamcrest Matchers 验证 JSON 数组大小为 2
                .andExpect(jsonPath("$[0].id", is(testPrompt1.getId().intValue()))) // 断言：第一个元素的 id 正确 (注意 Long -> int 转换)
                .andExpect(jsonPath("$[0].title", is(testPrompt1.getTitle()))) // 断言：第一个元素的 title 正确
                .andExpect(jsonPath("$[1].id", is(testPrompt2.getId().intValue()))) // 断言：第二个元素的 id 正确
                .andExpect(jsonPath("$[1].title", is(testPrompt2.getTitle()))); // 断言：第二个元素的 title 正确
    }

    /**
     * 测试获取当前用户的所有 Prompts (GET /api/prompts) - 用户无 Prompt 场景。
     * 预期: 返回 200 OK 和一个空的 JSON 数组。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void getCurrentUserPrompts_NoPrompts() throws Exception {
        // Arrange: 配置模拟 promptService 返回空列表
        when(promptService.getPromptsByUserId(testUser.getId())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get("/api/prompts")) // 模拟 GET 请求
                .andExpect(status().isOk()) // 断言：期望状态码 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 断言：期望内容类型 JSON
                .andExpect(jsonPath("$", hasSize(0))); // 断言：期望 JSON 数组大小为 0
    }

    /**
     * 测试根据 ID 获取单个 Prompt (GET /api/prompts/{id}) - 成功场景。
     * 预期: 返回 200 OK 和对应的 Prompt 实体 JSON。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void getPromptById_Success() throws Exception {
        // Arrange: 配置模拟 promptService 返回指定的 Prompt 实体
        // 使用 eq() 精确匹配 ID 和用户 ID
        when(promptService.getPromptById(eq(testPrompt1.getId()), eq(testUser.getId()))).thenReturn(testPrompt1);

        // Act & Assert
        mockMvc.perform(get("/api/prompts/{id}", testPrompt1.getId())) // 模拟 GET 请求，传入路径变量 id
                .andExpect(status().isOk()) // 断言：期望状态码 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 断言：期望内容类型 JSON
                .andExpect(jsonPath("$.id", is(testPrompt1.getId().intValue()))) // 断言：响应体 id 正确
                .andExpect(jsonPath("$.title", is(testPrompt1.getTitle()))) // 断言：响应体 title 正确
                .andExpect(jsonPath("$.content", is(testPrompt1.getContent()))); // 断言：响应体 content 正确
    }

    /**
     * 测试根据 ID 获取单个 Prompt (GET /api/prompts/{id}) - 未找到场景。
     * 预期: 返回 404 Not Found。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void getPromptById_NotFound() throws Exception {
        // Arrange: 准备一个不存在的 ID
        Long nonExistentId = 999L;
        // 配置模拟 promptService：当查询此 ID 时返回 null
        when(promptService.getPromptById(eq(nonExistentId), eq(testUser.getId()))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/prompts/{id}", nonExistentId)) // 模拟 GET 请求
                .andExpect(status().isNotFound()); // 断言：期望状态码 404 Not Found (由 Controller 判断 service 返回 null 并返回 404)
    }

    /**
     * 测试创建 Prompt (POST /api/prompts) - 成功场景。
     * 预期: 返回 201 Created 状态码和创建后的 Prompt 实体 JSON。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void createPrompt_Success() throws Exception {
        // Arrange: 准备用于请求体的 Prompt 数据 (ID 和 userId 通常由后端设置，因此这里为 null)
        Prompt newPromptData = new Prompt(null, null, "New Prompt", "New Content", "New Cat", null, null);
        // 准备模拟 Service 层成功创建后返回的 Prompt 实体
        Prompt createdPrompt = new Prompt(103L, testUser.getId(), "New Prompt", "New Content", "New Cat", LocalDateTime.now(), LocalDateTime.now());

        // 配置模拟 promptService：当调用 createPrompt 方法 (匹配任何 Prompt 对象和用户 ID) 时，返回上面准备的 createdPrompt 实体
        // 使用 any(Prompt.class) 匹配第一个参数，因为 Controller 会将请求体 JSON 反序列化为 Prompt 对象传入
        when(promptService.createPrompt(any(Prompt.class), eq(testUser.getId()))).thenReturn(createdPrompt);

        // Act & Assert
        mockMvc.perform(post("/api/prompts") // 模拟 POST 请求
                        .with(csrf()) // 添加 CSRF token
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求类型
                        .content(objectMapper.writeValueAsString(newPromptData))) // 将请求数据序列化为 JSON 请求体
                .andExpect(status().isCreated()) // 断言：期望状态码 201 Created
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 断言：期望内容类型 JSON
                .andExpect(jsonPath("$.id", is(createdPrompt.getId().intValue()))) // 断言：响应体 id 正确
                .andExpect(jsonPath("$.title", is(createdPrompt.getTitle()))) // 断言：响应体 title 正确
                .andExpect(jsonPath("$.userId", is(testUser.getId().intValue()))); // **重要**: 确认响应体中 userId 被正确设置
    }

    /**
     * 测试更新 Prompt (PUT /api/prompts/{id}) - 成功场景。
     * 预期: 返回 200 OK 状态码和更新后的 Prompt 实体 JSON。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void updatePrompt_Success() throws Exception {
        // Arrange: 准备要更新的 Prompt ID
        Long existingId = testPrompt1.getId();
        // 准备用于请求体的更新数据
        Prompt updatedPromptData = new Prompt(null, null, "Updated Title", "Updated Content", "Updated Cat", null, null);
        // 准备模拟 Service 层成功更新后返回的 Prompt 实体 (ID 不变，内容更新，userId 应保持)
        Prompt updatedPromptResult = new Prompt(existingId, testUser.getId(), "Updated Title", "Updated Content", "Updated Cat", testPrompt1.getCreatedAt(), LocalDateTime.now());

        // 配置模拟 promptService：当调用 updatePrompt 方法 (匹配 ID, 任何 Prompt 对象, 用户 ID) 时，返回上面准备的 updatedPromptResult
        when(promptService.updatePrompt(eq(existingId), any(Prompt.class), eq(testUser.getId()))).thenReturn(updatedPromptResult);

        // Act & Assert
        mockMvc.perform(put("/api/prompts/{id}", existingId) // 模拟 PUT 请求，传入路径变量 id
                        .with(csrf()) // 添加 CSRF token
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求类型
                        .content(objectMapper.writeValueAsString(updatedPromptData))) // 将更新数据序列化为 JSON 请求体
                .andExpect(status().isOk()) // 断言：期望状态码 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 断言：期望内容类型 JSON
                .andExpect(jsonPath("$.id", is(existingId.intValue()))) // 断言：响应体 id 保持不变
                .andExpect(jsonPath("$.title", is(updatedPromptResult.getTitle()))); // 断言：响应体 title 已更新
    }

    /**
     * 测试更新 Prompt (PUT /api/prompts/{id}) - 未找到场景。
     * 预期: 返回 404 Not Found。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void updatePrompt_NotFound() throws Exception {
        // Arrange: 准备一个不存在的 ID
        Long nonExistentId = 999L;
        // 准备更新数据
        Prompt updatedPromptData = new Prompt(null, null, "Update Non Existent", "Content", "Cat", null, null);

        // 配置模拟 promptService：当尝试更新此 ID 时返回 null
        when(promptService.updatePrompt(eq(nonExistentId), any(Prompt.class), eq(testUser.getId()))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(put("/api/prompts/{id}", nonExistentId) // 模拟 PUT 请求
                        .with(csrf()) // 添加 CSRF token
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求类型
                        .content(objectMapper.writeValueAsString(updatedPromptData))) // 设置请求体
                .andExpect(status().isNotFound()); // 断言：期望状态码 404 Not Found (由 Controller 判断 service 返回 null 并返回 404)
    }

    /**
     * 测试删除 Prompt (DELETE /api/prompts/{id}) - 成功场景。
     * 预期: 返回 204 No Content 状态码。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void deletePrompt_Success() throws Exception {
        // Arrange: 准备要删除的 Prompt ID
        Long existingId = testPrompt1.getId();
        // 配置模拟 promptService：当调用 deletePrompt (匹配 ID 和用户 ID) 时，返回 true (表示删除成功)
        when(promptService.deletePrompt(eq(existingId), eq(testUser.getId()))).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/prompts/{id}", existingId) // 模拟 DELETE 请求，传入路径变量 id
                        .with(csrf())) // 添加 CSRF token
                .andExpect(status().isNoContent()); // 断言：期望状态码 204 No Content
    }

    /**
     * 测试删除 Prompt (DELETE /api/prompts/{id}) - 未找到场景。
     * 预期: 返回 404 Not Found。
     * @throws Exception 测试异常
     */
    @Test
    @WithMockUser(username = "testuser") // 模拟认证用户
    void deletePrompt_NotFound() throws Exception {
        // Arrange: 准备一个不存在的 ID
        Long nonExistentId = 999L;
        // 配置模拟 promptService：当尝试删除此 ID 时返回 false
        when(promptService.deletePrompt(eq(nonExistentId), eq(testUser.getId()))).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/prompts/{id}", nonExistentId) // 模拟 DELETE 请求
                        .with(csrf())) // 添加 CSRF token
                .andExpect(status().isNotFound()); // 断言：期望状态码 404 Not Found
    }
} 