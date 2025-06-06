package com.ding.aiplatjava.service.impl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ding.aiplatjava.entity.Prompt;
import com.ding.aiplatjava.mapper.PromptMapper;

/**
 * Unit tests for {@link PromptServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class PromptServiceImplTest {

    @Mock
    private PromptMapper promptMapper; // Mock 依赖的 Mapper

    @InjectMocks
    private PromptServiceImpl promptService; // 注入 Mock，创建被测对象

    private Prompt prompt1;
    private Prompt prompt2;
    private final Long userId = 1L;
    private final Long otherUserId = 2L;
    private final Long promptId1 = 10L;
    private final Long promptId2 = 11L;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        LocalDateTime now = LocalDateTime.now();
        prompt1 = new Prompt(promptId1, userId, "Test Prompt 1", "Content 1", "Test", now, now);
        prompt2 = new Prompt(promptId2, userId, "Test Prompt 2", "Content 2", "Test", now.minusDays(1), now.minusDays(1));
    }

    // --- Test getPromptById --- 

    @Test
    void getPromptById_Success() {
        // Given: 当调用 selectById 且 ID 存在时，返回 prompt1
        when(promptMapper.selectById(promptId1)).thenReturn(prompt1);

        // When: 调用 service 方法
        Prompt found = promptService.getPromptById(promptId1, userId);

        // Then: 验证返回的 prompt 不为 null 且 ID 匹配，并验证 mapper 被调用一次
        assertNotNull(found);
        assertEquals(promptId1, found.getId());
        assertEquals(userId, found.getUserId());
        verify(promptMapper, times(1)).selectById(promptId1);
    }

    @Test
    void getPromptById_NotFound() {
        // Given: 当调用 selectById 时，返回 null (模拟找不到)
        when(promptMapper.selectById(promptId1)).thenReturn(null);

        // When: 调用 service 方法
        Prompt found = promptService.getPromptById(promptId1, userId);

        // Then: 验证返回值为 null
        assertNull(found);
        verify(promptMapper, times(1)).selectById(promptId1);
    }

    @Test
    void getPromptById_WrongUser() {
        // Given: 当调用 selectById 时，返回 prompt1 (属于 userId=1)
        when(promptMapper.selectById(promptId1)).thenReturn(prompt1);

        // When: 使用另一个用户 ID (otherUserId=2) 调用 service 方法
        Prompt found = promptService.getPromptById(promptId1, otherUserId);

        // Then: 验证返回值为 null (因为用户 ID 不匹配)
        assertNull(found);
        verify(promptMapper, times(1)).selectById(promptId1);
    }

    // --- Test getPromptsByUserId --- 

    @Test
    void getPromptsByUserId_Success() {
        // Given: 当调用 selectByUserId 时，返回包含 prompt1 和 prompt2 的列表
        List<Prompt> userPrompts = Arrays.asList(prompt1, prompt2);
        when(promptMapper.selectByUserId(userId)).thenReturn(userPrompts);

        // When: 调用 service 方法
        List<Prompt> foundList = promptService.getPromptsByUserId(userId);

        // Then: 验证返回的列表不为空，大小为 2，且包含正确的 prompt
        assertNotNull(foundList);
        assertEquals(2, foundList.size());
        assertTrue(foundList.contains(prompt1));
        assertTrue(foundList.contains(prompt2));
        verify(promptMapper, times(1)).selectByUserId(userId);
    }

    @Test
    void getPromptsByUserId_NoPrompts() {
        // Given: 当调用 selectByUserId 时，返回空列表
        when(promptMapper.selectByUserId(userId)).thenReturn(Collections.emptyList());

        // When: 调用 service 方法
        List<Prompt> foundList = promptService.getPromptsByUserId(userId);

        // Then: 验证返回的列表不为空，但大小为 0 
        assertNotNull(foundList);
        assertTrue(foundList.isEmpty());
        verify(promptMapper, times(1)).selectByUserId(userId);
    }

    // --- Test createPrompt --- 

    @Test
    void createPrompt_Success() {
        // Given: 
        // 1. 要创建的 Prompt 数据 (无 ID 和时间戳)
        Prompt newPromptData = new Prompt(null, null, "New Title", "New Content", "New Cat", null, null);
        
        // 2. 预期的、插入后从数据库查询返回的 Prompt 对象状态
        //    (包含模拟的 ID 和时间戳，因为 Service 现在会重新查询)
        Long expectedId = 99L; // 假设一个 ID
        LocalDateTime expectedTimestamp = LocalDateTime.now(); 
        Prompt expectedReturnedPrompt = new Prompt(expectedId, userId, "New Title", "New Content", "New Cat", expectedTimestamp, expectedTimestamp);

        // 3. 模拟 Mapper 行为:
        //    - 模拟 insert (可以不指定返回值，但要确保 prompt 对象被捕获以便后续验证 ID)
        //      或者更简单的方式是：假设 insert 成功
        //    - **关键：模拟 selectById 调用**，当 Service 在 insert 后调用 selectById 时，返回我们预期的对象
        //      注意：这里使用 any(Long.class) 因为我们无法轻易预测 insert 后生成的 ID。
        //      更精确的做法是使用 ArgumentCaptor 捕获 insert 时的 prompt 对象，获取 ID 再模拟，但 any() 更简单。
        //      为了让 selectById 返回预设的ID，我们假设insert后 getId() 返回 expectedId
        //      这需要在调用 Service 前准备好 newPromptData (虽然 Service 内部会重新 select)
        newPromptData.setId(expectedId); // 临时设置，以便 Service 调用 selectById(expectedId)
        when(promptMapper.selectById(expectedId)).thenReturn(expectedReturnedPrompt);
        // 也可以只模拟 insert 返回 1，但这不影响 selectById 的模拟
        // when(promptMapper.insert(any(Prompt.class))).thenReturn(1);

        // When: 调用 service 方法创建 Prompt
        // 传入的是 newPromptData，但 Service 内部的逻辑是 insert -> selectById
        Prompt created = promptService.createPrompt(newPromptData, userId);

        // Then: 
        // 1. 验证返回的对象不为 null，并且等于我们预期的返回对象
        assertNotNull(created);
        assertEquals(expectedReturnedPrompt, created);
        assertEquals(expectedId, created.getId()); // 校验ID
        assertEquals(userId, created.getUserId()); // 校验UserID
        assertNotNull(created.getCreatedAt()); // 校验时间戳非空
        assertNotNull(created.getUpdatedAt());

        // 2. 验证 mapper 的 insert 方法被调用了一次
        //    注意：传入 insert 的对象现在不包含时间戳了
        verify(promptMapper, times(1)).insert(argThat(p -> 
            Objects.equals(p.getUserId(), userId) && 
            Objects.equals(p.getTitle(), "New Title") && // 可以添加更多字段校验
            p.getCreatedAt() == null && // 确认 Service 层没有设置时间戳
            p.getUpdatedAt() == null
        ));
        
        // 3. 验证 mapper 的 selectById 方法也被调用了一次，并且 ID 是我们预期的
        verify(promptMapper, times(1)).selectById(expectedId);
    }

    // --- Test updatePrompt --- 

    @Test
    void updatePrompt_Success() {
        // Given: 
        // 1. 模拟第一次 selectById 找到现有的 prompt1
        Prompt originalPrompt = new Prompt(promptId1, userId, "Test Prompt 1", "Content 1", "Test", LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1)); // 确保时间戳不同
        when(promptMapper.selectById(promptId1)).thenReturn(originalPrompt);
        
        // 2. 模拟 updateById 操作成功 (返回影响行数 1)
        when(promptMapper.updateById(any(Prompt.class))).thenReturn(1);
        
        // 3. 准备一个模拟的"更新后"的状态，这个状态将由第二次 selectById 返回
        //    这个状态的 updated_at 应该与 originalPrompt 不同，且与 service 内部设置的 now() 也不同
        LocalDateTime distinctUpdateTime = LocalDateTime.now().plusMinutes(5); // 确保这个时间戳是不同的
        Prompt updatedDbState = new Prompt(promptId1, userId, "Updated Title", "Updated Content", "Updated Cat", originalPrompt.getCreatedAt(), distinctUpdateTime);
        //   让第二次调用 selectById 返回这个更新后的状态
        when(promptMapper.selectById(promptId1)).thenReturn(originalPrompt) // 第一次调用返回原始状态
                                         .thenReturn(updatedDbState);   // 第二次调用返回更新后的状态

        // 准备传入 service 的更新数据
        Prompt updateDetails = new Prompt(null, null, "Updated Title", "Updated Content", "Updated Cat", null, null);

        // When: 调用 service 方法更新
        Prompt updated = promptService.updatePrompt(promptId1, updateDetails, userId);

        // Then: 
        // 验证返回的 prompt 不为 null，并且是模拟的更新后状态 (updatedDbState)
        assertNotNull(updated);
        assertEquals(updatedDbState, updated); // 验证返回的是模拟的更新后状态
        assertEquals(promptId1, updated.getId());
        assertEquals(userId, updated.getUserId());
        assertEquals("Updated Title", updated.getTitle());
        assertEquals(distinctUpdateTime, updated.getUpdatedAt()); // 验证更新时间是模拟的更新后时间
        assertNotEquals(originalPrompt.getUpdatedAt(), updated.getUpdatedAt()); // 验证更新时间确实改变了
        
        // 验证 selectById 被调用两次
        verify(promptMapper, times(2)).selectById(promptId1);
        // 验证 updateById 被调用一次，并且传入的对象包含了正确的ID, UserID, 和更新的 Title
        // (简化对 updatedAt 的验证，只确保非 null)
        verify(promptMapper, times(1)).updateById(argThat(p ->
            Objects.equals(p.getId(), promptId1) &&
            Objects.equals(p.getUserId(), userId) &&
            Objects.equals(p.getTitle(), "Updated Title") &&
            Objects.equals(p.getContent(), "Updated Content") && // 也检查 content
            Objects.equals(p.getCategory(), "Updated Cat") && // 也检查 category
            p.getUpdatedAt() != null // 确保 service 设置了更新时间
        ));
    }

    @Test
    void updatePrompt_NotFound() {
        // Given: 模拟找不到要更新的 prompt
        when(promptMapper.selectById(promptId1)).thenReturn(null);
        Prompt updateDetails = new Prompt(null, null, "Update", "Update", "Update", null, null);

        // When: 调用 service 更新
        Prompt updated = promptService.updatePrompt(promptId1, updateDetails, userId);

        // Then: 验证返回值为 null，且 updateById 未被调用
        assertNull(updated);
        verify(promptMapper, times(1)).selectById(promptId1);
        verify(promptMapper, never()).updateById(any(Prompt.class));
    }

    @Test
    void updatePrompt_WrongUser() {
        // Given: 模拟找到 prompt1，但使用错误的用户 ID 去更新
        when(promptMapper.selectById(promptId1)).thenReturn(prompt1);
        Prompt updateDetails = new Prompt(null, null, "Update", "Update", "Update", null, null);

        // When: 调用 service 更新
        Prompt updated = promptService.updatePrompt(promptId1, updateDetails, otherUserId);

        // Then: 验证返回值为 null，且 updateById 未被调用
        assertNull(updated);
        verify(promptMapper, times(1)).selectById(promptId1);
        verify(promptMapper, never()).updateById(any(Prompt.class));
    }

    // --- Test deletePrompt --- 

    @Test
    void deletePrompt_Success() {
        // Given: 模拟 deleteById 成功，返回影响行数 1
        when(promptMapper.deleteById(promptId1, userId)).thenReturn(1);

        // When: 调用 service 删除
        boolean deleted = promptService.deletePrompt(promptId1, userId);

        // Then: 验证返回 true，且 deleteById 被正确调用
        assertTrue(deleted);
        verify(promptMapper, times(1)).deleteById(promptId1, userId);
    }

    @Test
    void deletePrompt_Failure() {
        // Given: 模拟 deleteById 失败 (ID 不存在或用户不匹配)，返回影响行数 0
        when(promptMapper.deleteById(promptId1, userId)).thenReturn(0);

        // When: 调用 service 删除
        boolean deleted = promptService.deletePrompt(promptId1, userId);

        // Then: 验证返回 false
        assertFalse(deleted);
        verify(promptMapper, times(1)).deleteById(promptId1, userId);
    }
} 