package com.ding.aiplatjava;

import com.ding.aiplatjava.dto.ApiTokenDto;
import com.ding.aiplatjava.dto.AuthResponseDto;
import com.ding.aiplatjava.dto.LoginRequestDto;
import com.ding.aiplatjava.dto.RegisterRequestDto;
import com.ding.aiplatjava.entity.Prompt; // 引入Prompt实体
// import com.ding.aiplatjava.entity.User; // 如果确实需要User实体进行设置或验证，则取消注释
// import com.ding.aiplatjava.mapper.UserMapper; // 如果需要直接创建用户，则取消注释
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
// import org.springframework.security.crypto.password.PasswordEncoder; // 如果需要直接进行用户设置，则取消注释
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 资源访问集成测试。
 * 测试跨用户访问资源（如API Token、Prompt）的安全性。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test") // 激活 application-test.properties 配置文件
@Transactional // 每个测试方法将在一个事务中运行，并在结束后回滚
public class ResourceAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 如果在更复杂的场景中决定以编程方式创建用户，可能需要UserMapper和PasswordEncoder，
    // 但目前我们将依赖于注册流程。
    // @Autowired
    // private UserMapper userMapper;
    // @Autowired
    // private PasswordEncoder passwordEncoder;

    private String userAUsername;
    private String userAPassword;
    private String userAAuthToken;

    private String userBUsername;
    private String userBPassword;
    private String userBAuthToken;

    /**
     * 辅助方法：注册一个用户。
     * @param username 用户名
     * @param password 密码
     * @throws Exception 执行过程中的异常
     */
    private void registerUser(String username, String password) throws Exception {
        RegisterRequestDto registerRequest = new RegisterRequestDto();
        registerRequest.setUsername(username);
        registerRequest.setEmail(username + "@example.com");
        registerRequest.setPassword(password);
        registerRequest.setConfirmPassword(password);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());
    }

    /**
     * 辅助方法：用户登录并返回JWT。
     * @param username 用户名
     * @param password 密码
     * @return JWT字符串
     * @throws Exception 执行过程中的异常
     */
    private String loginUserAndGetToken(String username, String password) throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponseDto authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponseDto.class);
        return authResponse.getAccessToken();
    }

    /**
     * 辅助方法：为当前认证用户创建一个API Token。
     * 假设用于请求的ApiTokenDto包含'provider'和'tokenValue'。
     * @param authToken 用户的认证Token
     * @param provider AI服务提供商
     * @param tokenValue Token的值
     * @return 创建的Token的ID。
     * @throws Exception 执行过程中的异常
     */
    private Long createApiTokenForCurrentUser(String authToken, String provider, String tokenValue) throws Exception {
        ApiTokenDto createTokenRequest = new ApiTokenDto();
        createTokenRequest.setProvider(provider);
        createTokenRequest.setTokenValue(tokenValue); // 假设tokenValue是创建DTO的一部分

        MvcResult createResult = mockMvc.perform(post("/api/tokens")
                        .with(csrf())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createTokenRequest)))
                .andExpect(status().isCreated()) // 假设Token创建成功返回201 Created
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        
        ApiTokenDto createdToken = objectMapper.readValue(createResult.getResponse().getContentAsString(), ApiTokenDto.class);
        return createdToken.getId();
    }

    /**
     * 辅助方法：为当前认证用户创建一个Prompt。
     * @param authToken 用户的认证Token
     * @param title Prompt标题
     * @param content Prompt内容
     * @param category Prompt分类
     * @return 创建的Prompt的ID。
     * @throws Exception 执行过程中的异常
     */
    private Long createPromptForCurrentUser(String authToken, String title, String content, String category) throws Exception {
        Prompt newPrompt = new Prompt();
        newPrompt.setTitle(title);
        newPrompt.setContent(content);
        newPrompt.setCategory(category);
        // userId 将由后端根据 authToken 设置，不需要前端传递

        MvcResult createResult = mockMvc.perform(post("/api/prompts")
                        .with(csrf())
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPrompt)))
                .andExpect(status().isCreated()) // 假设Prompt创建成功返回201 Created
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        
        Prompt createdPrompt = objectMapper.readValue(createResult.getResponse().getContentAsString(), Prompt.class);
        return createdPrompt.getId();
    }


    @BeforeEach
    void setUp() throws Exception {
        userAUsername = "userA_" + System.currentTimeMillis();
        userAPassword = "PasswordA123!";
        registerUser(userAUsername, userAPassword);
        userAAuthToken = loginUserAndGetToken(userAUsername, userAPassword);

        userBUsername = "userB_" + System.currentTimeMillis();
        userBPassword = "PasswordB456!";
        registerUser(userBUsername, userBPassword);
        userBAuthToken = loginUserAndGetToken(userBUsername, userBPassword);
    }

    @Test
    @DisplayName("API Token - 跨用户访问被禁止")
    void testApiToken_CrossUserAccessIsForbidden() throws Exception {
        // 1. 用户A创建TokenA
        Long tokenAId = createApiTokenForCurrentUser(userAAuthToken, "openai-userA", "tokenValueA");
        assertThat(tokenAId).isNotNull();

        // 2. 用户B创建TokenB
        Long tokenBId = createApiTokenForCurrentUser(userBAuthToken, "openai-userB", "tokenValueB");
        assertThat(tokenBId).isNotNull();

        // ---- 测试场景 ----

        // 场景 1: 用户A尝试获取用户B的Token列表 (应该只获取到自己的)
        // /api/tokens (GET) 端点应该只返回认证用户的Token。
        // 因此，用户A访问 /api/tokens 时不应看到 tokenBId。
        mockMvc.perform(get("/api/tokens")
                        .header("Authorization", "Bearer " + userAAuthToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + tokenBId + ")]").doesNotExist()) // 断言 tokenBId 不在用户A的列表中
                .andExpect(jsonPath("$[?(@.id == " + tokenAId + ")]").exists());      // 断言 tokenAId 在用户A的列表中


        // 场景 2: 用户A尝试删除用户B的TokenB (应该失败)
        // 如果用户尝试删除一个不属于他们的Token，我们预期返回403 Forbidden或404 Not Found。
        // 确切的状态码取决于您的ApiTokenController对此类情况的实现。
        // 根据用户反馈，此处应为 404 Not Found。
        mockMvc.perform(delete("/api/tokens/" + tokenBId)
                        .with(csrf()) // DELETE 请求通常需要CSRF
                        .header("Authorization", "Bearer " + userAAuthToken))
                .andExpect(status().isNotFound()); // 修改为 isNotFound()

        // 验证TokenB仍然存在 (用户B可以获取它)
        mockMvc.perform(get("/api/tokens") // 用户B获取其Token列表
                        .header("Authorization", "Bearer " + userBAuthToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + tokenBId + ")]").exists()); // TokenB应该仍在用户B的列表中
    }

    @Test
    @DisplayName("Prompt - 跨用户访问被禁止")
    void testPrompt_CrossUserAccessIsForbidden() throws Exception {
        // 1. 用户A创建PromptA
        Long promptAId = createPromptForCurrentUser(userAAuthToken, "用户A的标题", "用户A的内容", "通用");
        assertThat(promptAId).isNotNull();

        // 2. 用户B创建PromptB
        Long promptBId = createPromptForCurrentUser(userBAuthToken, "用户B的标题", "用户B的内容", "测试");
        assertThat(promptBId).isNotNull();

        // ---- 测试场景 ----

        // 场景 1: 用户A尝试获取用户B的Prompt列表 (应该只获取到自己的)
        mockMvc.perform(get("/api/prompts")
                        .header("Authorization", "Bearer " + userAAuthToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + promptBId + ")]").doesNotExist()) // 断言 promptBId 不在用户A的列表中
                .andExpect(jsonPath("$[?(@.id == " + promptAId + ")]").exists());      // 断言 promptAId 在用户A的列表中

        // 场景 2: 用户A尝试获取用户B的特定PromptB (应该失败)
        mockMvc.perform(get("/api/prompts/" + promptBId)
                        .header("Authorization", "Bearer " + userAAuthToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()); // 假设与Token逻辑一致，返回404

        // 场景 3: 用户A尝试更新用户B的PromptB (应该失败)
        Prompt updatedPromptData = new Prompt();
        updatedPromptData.setTitle("由用户A修改的标题");
        updatedPromptData.setContent("由用户A修改的内容");
        updatedPromptData.setCategory("恶意更新");

        mockMvc.perform(put("/api/prompts/" + promptBId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + userAAuthToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPromptData)))
                .andExpect(status().isNotFound()); // 假设与Token逻辑一致，返回404

        // 验证PromptB未被修改 (用户B可以获取其原始信息)
        mockMvc.perform(get("/api/prompts/" + promptBId)
                        .header("Authorization", "Bearer " + userBAuthToken) // 用户B获取自己的Prompt
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("用户B的标题"))
                .andExpect(jsonPath("$.content").value("用户B的内容"));

        // 场景 4: 用户A尝试删除用户B的PromptB (应该失败)
        mockMvc.perform(delete("/api/prompts/" + promptBId)
                        .with(csrf())
                        .header("Authorization", "Bearer " + userAAuthToken))
                .andExpect(status().isNotFound()); // 假设与Token逻辑一致，返回404

        // 验证PromptB仍然存在 (用户B可以获取它)
        mockMvc.perform(get("/api/prompts/" + promptBId)
                        .header("Authorization", "Bearer " + userBAuthToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
} 