package com.ding.aiplatjava;

import com.ding.aiplatjava.dto.AuthResponseDto;
import com.ding.aiplatjava.dto.LoginRequestDto;
import com.ding.aiplatjava.dto.RegisterRequestDto;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.mapper.UserMapper; // 假设你有一个 UserMapper 或 UserRepository
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional; // 用于测试后回滚数据库

import static org.assertj.core.api.Assertions.assertThat; // 使用 AssertJ 进行断言
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // Import for GET requests
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content; // Import for content assertions

/**
 * 用户认证流程集成测试。
 * 测试用户注册、登录以及JWT认证访问受保护资源。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // 加载完整Spring上下文，使用随机端口
@AutoConfigureMockMvc // 自动配置 MockMvc
@ActiveProfiles("test") // 激活 application-test.properties 配置文件
@Transactional // 每个测试方法将在一个事务中运行，并在结束后回滚，保持数据库清洁
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // 用于模拟 HTTP 请求

    @Autowired
    private ObjectMapper objectMapper; // 用于 JSON 序列化/反序列化

    @Autowired
    private UserMapper userMapper; // 或 UserRepository，用于数据库验证

    @Autowired
    private PasswordEncoder passwordEncoder; // 用于验证密码加密

    /**
     * 集成测试场景 1.1: 成功注册新用户并登录。
     * 步骤:
     * 1. 发送注册请求。
     * 2. 验证注册成功响应 (201 Created)。
     * 3. 数据库验证用户已创建且密码已加密。
     * 4. 使用新凭证登录。
     * 5. 验证登录成功响应 (200 OK, 返回 accessToken)。
     */
    @Test
    @DisplayName("集成测试 - 成功注册新用户并登录")
    void registerAndLogin_Success() throws Exception {
        // ---- 1. 注册新用户 ----
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        String username = "testuser_" + System.currentTimeMillis(); // 保证用户名唯一
        String email = username + "@example.com";
        String rawPassword = "Password123!";

        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(rawPassword);
        registerRequest.setConfirmPassword(rawPassword); // 确保确认密码一致

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf()) // 集成测试包含安全过滤器，POST需要CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated()) // 验证状态码 201 Created
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));

        // ---- 2. 数据库验证 ----
        // 假设 UserMapper 有 findByUsername 方法，或者 UserRepository
        User savedUser = userMapper.selectByUsername(username);
        assertThat(savedUser).isNotNull(); // 验证用户已保存到数据库
        assertThat(savedUser.getEmail()).isEqualTo(email); // 验证邮箱正确
        assertThat(passwordEncoder.matches(rawPassword, savedUser.getPassword())).isTrue(); // 验证密码已加密且匹配

        // ---- 3. 使用新凭证登录 ----
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(rawPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk()) // 验证状态码 200 OK
                .andExpect(jsonPath("$.accessToken").exists()) // 验证响应中包含 accessToken
                .andReturn();

        // ---- 4. (可选) 提取并简单验证 accessToken ----
        String responseString = loginResult.getResponse().getContentAsString();
        AuthResponseDto authResponse = objectMapper.readValue(responseString, AuthResponseDto.class);
        assertThat(authResponse.getAccessToken()).isNotBlank(); // 验证 token 不为空

        // 后续可以添加更多场景的测试方法 (登录失败、用户名已存在等)
    }

    /**
     * 集成测试场景 1.2: 使用无效凭证登录失败。
     * 包含两种情况:
     * 1. 用户名不存在。
     * 2. 用户名存在但密码错误。
     */
    @Test
    @DisplayName("集成测试 - 使用无效凭证登录失败")
    void login_Failure_InvalidCredentials() throws Exception {
        // ---- 场景 1.2.1: 用户名不存在 ----
        LoginRequestDto loginRequestNonExistentUser = new LoginRequestDto();
        loginRequestNonExistentUser.setUsernameOrEmail("nonexistentuser" + System.currentTimeMillis());
        loginRequestNonExistentUser.setPassword("anypassword");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf()) // POST请求通常需要CSRF，即使全局禁用，保持一致性或根据具体配置移除
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestNonExistentUser)))
                .andExpect(status().isUnauthorized()); // 验证状态码 401 Unauthorized

        // ---- 场景 1.2.2: 用户名存在但密码错误 ----
        // 前置准备: 先注册一个用户
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        String username = "existinguser_" + System.currentTimeMillis();
        String email = username + "@example.com";
        String correctPassword = "CorrectPassword123!";
        String wrongPassword = "WrongPassword456!";

        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(correctPassword);
        registerRequest.setConfirmPassword(correctPassword);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 使用正确用户名和错误密码登录
        LoginRequestDto loginRequestWrongPassword = new LoginRequestDto();
        loginRequestWrongPassword.setUsernameOrEmail(username);
        loginRequestWrongPassword.setPassword(wrongPassword);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestWrongPassword)))
                .andExpect(status().isUnauthorized()); // 验证状态码 401 Unauthorized
    }

    /**
     * 集成测试场景 1.3: 注册时用户名已存在。
     * 步骤:
     * 1. 成功注册一个用户。
     * 2. 尝试使用相同的用户名再次注册。
     * 3. 验证第二次注册失败 (预期400 Bad Request或409 Conflict)。
     */
    @Test
    @DisplayName("集成测试 - 注册时用户名已存在")
    void register_Failure_UsernameAlreadyExists() throws Exception {
        // ---- 1. 首次成功注册用户 ----
        RegisterRequestDto registerRequestFirst = new RegisterRequestDto();
        String sharedUsername = "duplicateuser_" + System.currentTimeMillis();
        String firstEmail = sharedUsername + "_first@example.com";
        String password = "Password123!";

        registerRequestFirst.setUsername(sharedUsername);
        registerRequestFirst.setEmail(firstEmail);
        registerRequestFirst.setPassword(password);
        registerRequestFirst.setConfirmPassword(password);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestFirst)))
                .andExpect(status().isCreated());

        // ---- 2. 尝试使用相同用户名再次注册 ----
        RegisterRequestDto registerRequestSecond = new RegisterRequestDto();
        String secondEmail = sharedUsername + "_second@example.com"; // 使用不同的邮箱以确保是用户名冲突

        registerRequestSecond.setUsername(sharedUsername); // 相同的用户名
        registerRequestSecond.setEmail(secondEmail);
        registerRequestSecond.setPassword(password);
        registerRequestSecond.setConfirmPassword(password);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequestSecond)))
                .andExpect(status().isBadRequest()); // 假设控制器返回400, 如果是409则修改此处
    }

    /**
     * 集成测试场景 1.4: 使用 JWT 访问受保护资源 (/api/tokens)。
     * 包含三种情况:
     * 1. 成功访问: 使用有效JWT。
     * 2. 访问被拒 (无Token): 未提供JWT。
     * 3. 访问被拒 (无效Token): 提供无效或格式错误的JWT。
     */
    @Test
    @DisplayName("集成测试 - 使用JWT访问受保护资源")
    void accessProtectedResource_WithJwt() throws Exception {
        // ---- 前置准备: 注册并登录用户以获取有效JWT ----
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        String username = "jwtUser_" + System.currentTimeMillis();
        String email = username + "@example.com";
        String rawPassword = "PasswordForJwtTest123!";

        registerRequest.setUsername(username);
        registerRequest.setEmail(email);
        registerRequest.setPassword(rawPassword);
        registerRequest.setConfirmPassword(rawPassword);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(rawPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseString = loginResult.getResponse().getContentAsString();
        AuthResponseDto authResponse = objectMapper.readValue(responseString, AuthResponseDto.class);
        String validJwt = authResponse.getAccessToken();
        assertThat(validJwt).isNotBlank();

        // ---- 场景 1.4.1: 使用有效JWT成功访问受保护资源 (/api/tokens) ----
        // 首先，为该用户创建一个Token，以便列表不为空
        // (这里我们假设 ApiTokenDto 和 ApiTokenController 已经存在并按预期工作)
        // 为简化AuthIntegrationTest，这里不直接调用ApiToken的创建，
        // 而是预期/api/tokens在用户首次访问时返回空列表（也是一种有效场景）
        // 或者，如果应用逻辑是在用户注册时自动创建某些默认token，那么这里列表可能不为空。
        // 我们将简单地检查200 OK和返回的是一个JSON数组。
        mockMvc.perform(get("/api/tokens") // 受保护端点
                        .header("Authorization", "Bearer " + validJwt) // 设置JWT认证头
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // 验证状态码 200 OK
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)) // 验证响应内容类型
                .andExpect(jsonPath("$").isArray()); // 验证返回的是一个JSON数组 (即使是空数组)


        // ---- 场景 1.4.2: 未提供JWT访问受保护资源 (/api/tokens) ----
        mockMvc.perform(get("/api/tokens")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // 验证状态码 401 Unauthorized

        // ---- 场景 1.4.3: 使用无效/格式错误的JWT访问受保护资源 (/api/tokens) ----
        String invalidJwt = "Bearer an.invalid.jwt.token";
        mockMvc.perform(get("/api/tokens")
                        .header("Authorization", invalidJwt)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // 验证状态码 401 Unauthorized
    }

}