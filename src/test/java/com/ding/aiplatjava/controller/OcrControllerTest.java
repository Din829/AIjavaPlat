package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.OcrResponseDto;
import com.ding.aiplatjava.dto.OcrTaskStatusDto;
import com.ding.aiplatjava.dto.OcrUploadRequestDto;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.service.OcrService;
import com.ding.aiplatjava.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OCR控制器测试类
 */
@SpringBootTest
public class OcrControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private OcrService ocrService;

    @MockBean
    private UserService userService;

    private User testUser;
    private String testTaskId;
    private final Long userId = 1L;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();

        // 创建测试用户
        testUser = new User();
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, userId);

            Field usernameField = User.class.getDeclaredField("username");
            usernameField.setAccessible(true);
            usernameField.set(testUser, "user");

            Field emailField = User.class.getDeclaredField("email");
            emailField.setAccessible(true);
            emailField.set(testUser, "test@example.com");
        } catch (Exception e) {
            throw new RuntimeException("设置用户字段失败", e);
        }

        // 设置测试任务ID
        testTaskId = "test-task-id";

        // 模拟用户服务
        when(userService.findByUsername("user")).thenReturn(Optional.of(testUser));
    }

    /**
     * 测试上传文件
     */
    @Test
    @WithMockUser(username = "user")
    public void testUploadFile() throws Exception {
        // 创建模拟文件
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            MediaType.APPLICATION_PDF_VALUE,
            "PDF content".getBytes()
        );

        // 创建响应DTO
        OcrResponseDto responseDto = new OcrResponseDto(testTaskId, "PENDING");

        // 模拟服务
        when(ocrService.uploadAndProcess(any(), eq(userId), any(OcrUploadRequestDto.class)))
            .thenReturn(responseDto);

        // 执行请求并验证
        mockMvc.perform(multipart("/api/ocr/upload")
                .file(file)
                .param("usePypdf2", "true")
                .param("useDocling", "true")
                .param("useGemini", "true")
                .param("forceOcr", "false")
                .param("language", "auto"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(testTaskId))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * 测试获取任务状态
     */
    @Test
    @WithMockUser(username = "user")
    public void testGetTaskStatus() throws Exception {
        // 创建状态DTO
        OcrTaskStatusDto statusDto = new OcrTaskStatusDto(testTaskId, "PROCESSING", "任务处理中");

        // 模拟服务
        when(ocrService.getTaskStatus(testTaskId, userId)).thenReturn(statusDto);

        // 执行请求并验证
        mockMvc.perform(get("/api/ocr/tasks/{taskId}/status", testTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(testTaskId))
            .andExpect(jsonPath("$.status").value("PROCESSING"))
            .andExpect(jsonPath("$.message").value("任务处理中"));
    }

    /**
     * 测试获取任务结果
     */
    @Test
    @WithMockUser(username = "user")
    public void testGetTaskResult() throws Exception {
        // 创建响应DTO
        OcrResponseDto responseDto = new OcrResponseDto(testTaskId, "COMPLETED");

        // 模拟服务
        when(ocrService.getTaskResult(testTaskId, userId)).thenReturn(responseDto);

        // 执行请求并验证
        mockMvc.perform(get("/api/ocr/tasks/{taskId}", testTaskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").value(testTaskId))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    /**
     * 测试获取用户任务列表
     */
    @Test
    @WithMockUser(username = "user")
    public void testGetUserTasks() throws Exception {
        // 创建任务列表
        List<OcrResponseDto> tasks = new ArrayList<>();
        OcrResponseDto task1 = new OcrResponseDto("task-1", "COMPLETED");
        OcrResponseDto task2 = new OcrResponseDto("task-2", "PROCESSING");
        tasks.add(task1);
        tasks.add(task2);

        // 模拟服务
        when(ocrService.getUserTasks(userId)).thenReturn(tasks);

        // 执行请求并验证
        mockMvc.perform(get("/api/ocr/tasks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].taskId").value("task-1"))
            .andExpect(jsonPath("$[0].status").value("COMPLETED"))
            .andExpect(jsonPath("$[1].taskId").value("task-2"))
            .andExpect(jsonPath("$[1].status").value("PROCESSING"));
    }
}
