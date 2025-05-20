package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.AuthResponseDto;
import com.ding.aiplatjava.dto.LoginRequestDto;
import com.ding.aiplatjava.dto.RegisterRequestDto;
import com.ding.aiplatjava.service.UserService;
import com.ding.aiplatjava.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 的单元测试类。
 * 主要测试用户登录和注册接口的功能是否符合预期。
 * 使用 @WebMvcTest 注解，专注于测试 Controller 层，不加载完整的 Spring 上下文。
 * 使用 @AutoConfigureMockMvc(addFilters = false) 禁用 Spring Security 过滤器，以便专注于 Controller 逻辑。
 */
@WebMvcTest(AuthController.class) // 指定测试目标 Controller
@AutoConfigureMockMvc(addFilters = false) // 禁用 Spring Security 过滤器链
class AuthControllerTest {

    // 自动注入 MockMvc，用于模拟 HTTP 请求
    @Autowired
    private MockMvc mockMvc;

    // @MockBean 创建并注入 Mockito mock 对象，替换 Spring 应用上下文中的真实 Bean
    // 模拟认证管理器，Controller 会用它来处理登录认证
    @MockBean
    private AuthenticationManager authenticationManager;

    // 模拟 JWT 工具类，Controller 会用它生成 Token
    @MockBean
    private JwtUtil jwtUtil;

    // 模拟用户服务，Controller 会用它处理注册逻辑
    @MockBean
    private UserService userService;

    // 自动注入 ObjectMapper，用于将对象序列化为 JSON 字符串
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 测试登录接口 (/api/auth/login) - 认证成功场景。
     * 预期: 返回 200 OK 状态码和包含 accessToken 的 JSON 响应体。
     * @throws Exception MockMvc 执行请求时可能抛出的异常
     */
    @Test // 标记这是一个 JUnit 5 测试方法
    void login_Success() throws Exception {
        // Arrange: 准备阶段，设置测试数据和模拟对象的行为
        LoginRequestDto loginRequest = new LoginRequestDto(); // 创建登录请求 DTO
        loginRequest.setUsernameOrEmail("testuser"); // 设置用户名或邮箱
        loginRequest.setPassword("password"); // 设置密码

        // 创建一个模拟的 UserDetails 对象，代表认证成功后的用户信息
        UserDetails userDetails = new User("testuser", "password", Collections.emptyList());
        // 模拟 Spring Security 的 Authentication 对象
        Authentication authentication = mock(Authentication.class);
        // 当调用 authentication.isAuthenticated() 时，返回 true
        when(authentication.isAuthenticated()).thenReturn(true);
        // 当调用 authentication.getPrincipal() 时，返回模拟的 userDetails
        when(authentication.getPrincipal()).thenReturn(userDetails);

        // 配置模拟的 authenticationManager：当调用 authenticate 方法（使用任何 UsernamePasswordAuthenticationToken 参数）时，返回上面模拟的 authentication 对象
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        // 配置模拟的 jwtUtil：当调用 generateToken 方法（使用任何 UserDetails 参数）时，返回一个预设的 mock token 字符串
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("mock-jwt-token");

        // Act & Assert: 执行阶段和断言阶段
        mockMvc.perform(post("/api/auth/login") // 模拟发送 POST 请求到 /api/auth/login
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求头的 Content-Type 为 application/json
                        .content(objectMapper.writeValueAsString(loginRequest))) // 将 loginRequest 对象转换为 JSON 字符串作为请求体
                .andExpect(status().isOk()) // 断言：期望 HTTP 响应状态码为 200 OK
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token")); // 断言：期望响应体 JSON 中 accessToken 字段的值为 "mock-jwt-token"
    }

    /**
     * 测试登录接口 (/api/auth/login) - 认证失败场景 (无效凭证)。
     * 预期: 返回 401 Unauthorized 状态码。
     * @throws Exception MockMvc 执行请求时可能抛出的异常
     */
    @Test
    void login_Failure_BadCredentials() throws Exception {
        // Arrange: 准备登录请求数据
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("wrongpassword"); // 使用错误的密码

        // 配置模拟的 authenticationManager：当调用 authenticate 方法时，抛出 BadCredentialsException 异常，模拟认证失败
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("无效的凭证"));

        // Act & Assert: 执行请求并验证
        mockMvc.perform(post("/api/auth/login") // 模拟 POST 请求
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求类型
                        .content(objectMapper.writeValueAsString(loginRequest))) // 设置请求体
                .andExpect(status().isUnauthorized()); // 断言：期望 HTTP 响应状态码为 401 Unauthorized (由 GlobalExceptionHandler 处理 BadCredentialsException)
    }

    /**
     * 测试注册接口 (/api/auth/register) - 成功场景。
     * 预期: 返回 201 Created 状态码和包含新用户信息的 JSON 响应体 (不含密码)。
     * @throws Exception MockMvc 执行请求时可能抛出的异常
     */
    @Test
    void register_Success() throws Exception {
        // Arrange: 准备注册请求数据
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123"); // 设置匹配的确认密码
        registerRequest.setEmail("newuser@example.com");

        // 准备一个模拟的 User 对象，代表 Service 层成功创建并返回的用户实体
        com.ding.aiplatjava.entity.User registeredUser = new com.ding.aiplatjava.entity.User();
        registeredUser.setId(1L); // 假设 ID 由数据库生成
        registeredUser.setUsername("newuser");
        registeredUser.setEmail("newuser@example.com");

        // 配置模拟的 userService：当调用 registerUser 方法（使用任何 User 对象作为参数）时，返回上面模拟的 registeredUser 对象
        when(userService.registerUser(any(com.ding.aiplatjava.entity.User.class))).thenReturn(registeredUser);

        // Act & Assert: 执行请求并验证
        mockMvc.perform(post("/api/auth/register") // 模拟 POST 请求
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求类型
                        .content(objectMapper.writeValueAsString(registerRequest))) // 设置请求体
                .andExpect(status().isCreated()) // 断言：期望 HTTP 响应状态码为 201 Created
                .andExpect(jsonPath("$.username").value("newuser")) // 断言：期望响应体 JSON 中 username 字段的值正确
                .andExpect(jsonPath("$.email").value("newuser@example.com")); // 断言：期望响应体 JSON 中 email 字段的值正确
    }

    /**
     * 测试注册接口 (/api/auth/register) - 失败场景 (用户名已存在)。
     * 假设 UserService 在用户名已存在时抛出 RuntimeException (或其他特定异常)，
     * 且 GlobalExceptionHandler 会将其映射为 400 Bad Request。
     * @throws Exception MockMvc 执行请求时可能抛出的异常
     */
    @Test
    void register_Failure_UserAlreadyExists() throws Exception {
        // Arrange: 准备注册请求数据 (用户名为已存在的用户)
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setUsername("existinguser");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("existing@example.com");

        // 配置模拟的 userService：当调用 registerUser 方法时，抛出 RuntimeException，模拟用户名冲突
        when(userService.registerUser(any(com.ding.aiplatjava.entity.User.class)))
                .thenThrow(new RuntimeException("用户名已存在"));

        // Act & Assert: 执行请求并验证
        mockMvc.perform(post("/api/auth/register") // 模拟 POST 请求
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求类型
                        .content(objectMapper.writeValueAsString(registerRequest))) // 设置请求体
                .andExpect(status().isBadRequest()); // 断言：期望 HTTP 响应状态码为 400 Bad Request (由 GlobalExceptionHandler 处理)
    }

    /**
     * 测试注册接口 (/api/auth/register) - 失败场景 (请求体验证失败，例如密码为空)。
     * Controller 方法参数使用了 @Valid (虽然这里没显示，但假设有)，Spring 会自动进行校验。
     * @throws Exception MockMvc 执行请求时可能抛出的异常
     */
    @Test
    void register_Failure_ValidationFailed() throws Exception {
        // Arrange: 准备一个包含无效数据的注册请求 DTO (密码为空)
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setUsername("validuser");
        registerRequest.setPassword(""); // 提供无效密码
        registerRequest.setEmail("valid@example.com");

        // Act & Assert: 执行请求并验证
        mockMvc.perform(post("/api/auth/register") // 模拟 POST 请求
                        .contentType(MediaType.APPLICATION_JSON) // 设置请求类型
                        .content(objectMapper.writeValueAsString(registerRequest))) // 设置请求体
                .andExpect(status().isBadRequest()); // 断言：期望 HTTP 响应状态码为 400 Bad Request (由 Spring Validation 自动处理并被 GlobalExceptionHandler 捕获)
    }
}