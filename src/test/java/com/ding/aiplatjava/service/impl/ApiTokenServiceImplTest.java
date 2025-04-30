package com.ding.aiplatjava.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ding.aiplatjava.entity.ApiToken;
import com.ding.aiplatjava.mapper.ApiTokenMapper;
import com.ding.aiplatjava.util.EncryptionUtil;

/**
 * {@link ApiTokenServiceImpl} 的单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ApiTokenServiceImplTest {

    @Mock
    private ApiTokenMapper apiTokenMapper; // 模拟 Mapper

    @Mock
    private EncryptionUtil encryptionUtil; // 模拟加密工具

    @InjectMocks
    private ApiTokenServiceImpl apiTokenService; // 注入 mocks

    private ApiToken token1;
    private final Long userId = 1L;// 用户 id
    private final Long tokenId1 = 10L;// token id
    private final String provider = "openai";// 提供商
    private final String plainTokenValue = "sk-plain-token-value";// 明文 token 值
    private final String encryptedTokenValue = "aes-encrypted-token-value"; // 模拟的加密值

    @BeforeEach
    // 设置测试数据
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        token1 = new ApiToken(tokenId1, userId, provider, encryptedTokenValue, now.minusDays(1), now.minusDays(1));
    }

    // --- 测试 createToken --- 

    @Test
    void createToken_Success() {
        // Arrange: 准备一个包含明文 Token 的输入对象
        ApiToken inputToken = new ApiToken();
        inputToken.setProvider(provider);
        inputToken.setTokenValue(plainTokenValue);

        // 模拟加密工具的行为
        when(encryptionUtil.encrypt(plainTokenValue)).thenReturn(encryptedTokenValue);
        // 模拟 Mapper 的 insert (用于验证调用)
        when(apiTokenMapper.insert(any(ApiToken.class))).thenReturn(1); // 返回影响行数1
        // (可选) 模拟 ID 生成: 如果 insert 方法会修改传入对象的 ID
        // doAnswer(invocation -> {
        //     ApiToken saved = invocation.getArgument(0);
        //     saved.setId(tokenId1); // 假设设置ID为10
        //     return 1; // insert 返回影响行数
        // }).when(apiTokenMapper).insert(any(ApiToken.class));
        
        // Act: 调用 createToken
        ApiToken createdToken = apiTokenService.createToken(inputToken, userId);

        // Assert: 
        // 1. 验证返回的 Token 不为 null
        assertNotNull(createdToken);
        // 2. 验证返回的 Token 的 tokenValue 是加密后的值
        assertEquals(encryptedTokenValue, createdToken.getTokenValue());
        // 3. 验证 userId, provider, 时间戳等是否正确设置
        assertEquals(userId, createdToken.getUserId());
        assertEquals(provider, createdToken.getProvider());
        assertNotNull(createdToken.getCreatedAt());
        assertNotNull(createdToken.getUpdatedAt());
        // 4. 验证加密工具的 encrypt 方法被调用了一次
        verify(encryptionUtil, times(1)).encrypt(plainTokenValue);
        // 5. 验证 Mapper 的 insert 方法被调用了一次，并且传入的 tokenValue 是加密后的
        ArgumentCaptor<ApiToken> tokenCaptor = ArgumentCaptor.forClass(ApiToken.class);// 捕获 ApiToken 对象，目的就是验证传入的 tokenValue 是加密后的
        verify(apiTokenMapper, times(1)).insert(tokenCaptor.capture());
        assertEquals(encryptedTokenValue, tokenCaptor.getValue().getTokenValue());// 验证传入的 tokenValue 是加密后的
        assertEquals(userId, tokenCaptor.getValue().getUserId());// 验证传入的 userId 是正确的
    }

    // --- 测试 getTokensByUserId --- 

    @Test
    void getTokensByUserId_Success() {
        // Arrange: 模拟 Mapper 返回包含 token1 的列表
        List<ApiToken> tokens = Collections.singletonList(token1);// 创建一个包含 token1 的列表
        when(apiTokenMapper.selectByUserId(userId)).thenReturn(tokens);// 模拟 Mapper 的 selectByUserId 方法返回包含 token1 的列表

        // Act: 调用 service 方法
        List<ApiToken> foundTokens = apiTokenService.getTokensByUserId(userId);// 调用 service 方法，获取包含 token1 的列表

        // Assert: 验证返回列表不为空且包含 token1 (其 tokenValue 应为加密状态)
        assertNotNull(foundTokens);// 验证返回列表不为空
        assertFalse(foundTokens.isEmpty());// 验证返回列表不为空
        assertEquals(1, foundTokens.size());// 验证返回列表大小为1
        assertEquals(token1, foundTokens.get(0));// 验证返回列表的第一个元素为 token1
        assertEquals(encryptedTokenValue, foundTokens.get(0).getTokenValue()); // 确认是加密值
        verify(apiTokenMapper, times(1)).selectByUserId(userId);// 验证 Mapper 的 selectByUserId 方法被调用了一次
    }

    // --- 测试 getDecryptedTokenValue --- 
    // 测试 getDecryptedTokenValue 方法，验证解密功能

    @Test
    void getDecryptedTokenValue_Success() {
        // Arrange: 模拟 Mapper 返回 token1，模拟解密工具返回明文
        when(apiTokenMapper.selectById(tokenId1)).thenReturn(token1);// 模拟 Mapper 的 selectById 方法返回 token1
        when(encryptionUtil.decrypt(encryptedTokenValue)).thenReturn(plainTokenValue);// 模拟解密工具的 decrypt 方法返回明文

        // Act: 调用 service 方法
        String decryptedValue = apiTokenService.getDecryptedTokenValue(tokenId1, userId);// 调用 service 方法，获取解密后的 tokenValue

        // Assert: 验证返回的解密值与预期明文一致
        assertEquals(plainTokenValue, decryptedValue);// 验证返回的解密值与预期明文一致
        verify(apiTokenMapper, times(1)).selectById(tokenId1);// 验证 Mapper 的 selectById 方法被调用了一次
        verify(encryptionUtil, times(1)).decrypt(encryptedTokenValue);// 验证解密工具的 decrypt 方法被调用了一次
    }

    // --- 测试TokenNotFound，解密失败 --- 

    @Test
    void getDecryptedTokenValue_TokenNotFound() {
        // Arrange: 模拟 Mapper 返回 null
        when(apiTokenMapper.selectById(tokenId1)).thenReturn(null);// 模拟 Mapper 的 selectById 方法返回 null

        // Act: 调用 service 方法
        String decryptedValue = apiTokenService.getDecryptedTokenValue(tokenId1, userId);// 调用 service 方法，获取解密后的 tokenValue

        // Assert: 验证返回 null，且解密方法未被调用
        assertNull(decryptedValue);// 验证返回 null
        verify(apiTokenMapper, times(1)).selectById(tokenId1);// 验证 Mapper 的 selectById 方法被调用了一次
        verify(encryptionUtil, never()).decrypt(anyString());// 验证解密工具的 decrypt 方法未被调用
    }

    // --- 测试WrongUser，解密失败 --- 
    @Test
    void getDecryptedTokenValue_WrongUser() {
        // Arrange: 模拟 Mapper 返回 token1 (属于 userId=1)
        when(apiTokenMapper.selectById(tokenId1)).thenReturn(token1);
        Long wrongUserId = 2L;

        // Act: 使用错误的用户 ID 调用 service 方法
        String decryptedValue = apiTokenService.getDecryptedTokenValue(tokenId1, wrongUserId);

        // Assert: 验证返回 null，且解密方法未被调用
        assertNull(decryptedValue);
        verify(apiTokenMapper, times(1)).selectById(tokenId1);
        verify(encryptionUtil, never()).decrypt(anyString());
    }

    @Test
    void getDecryptedTokenValue_DecryptionError() {
        // Arrange: 模拟 Mapper 返回 token1，模拟解密工具抛出异常
        when(apiTokenMapper.selectById(tokenId1)).thenReturn(token1);
        when(encryptionUtil.decrypt(encryptedTokenValue)).thenThrow(new RuntimeException("Decryption failed"));

        // Act: 调用 service 方法
        String decryptedValue = apiTokenService.getDecryptedTokenValue(tokenId1, userId);

        // Assert: 验证返回 null (因为 Service 内部捕获了异常)
        assertNull(decryptedValue);
        verify(apiTokenMapper, times(1)).selectById(tokenId1);
        verify(encryptionUtil, times(1)).decrypt(encryptedTokenValue);
    }

    // --- 测试 deleteToken --- 

    @Test
    void deleteToken_Success() {
        // Arrange: 模拟 Mapper 的 deleteByIdAndUserId 返回 1 (表示删除成功)
        when(apiTokenMapper.deleteByIdAndUserId(tokenId1, userId)).thenReturn(1);

        // Act: 调用 service 方法
        boolean deleted = apiTokenService.deleteToken(tokenId1, userId);

        // Assert: 验证返回 true，且 Mapper 方法被正确调用
        assertTrue(deleted);
        verify(apiTokenMapper, times(1)).deleteByIdAndUserId(tokenId1, userId);
    }

    @Test
    void deleteToken_Failure() {
        // Arrange: 模拟 Mapper 的 deleteByIdAndUserId 返回 0 (表示未找到或未删除)
        when(apiTokenMapper.deleteByIdAndUserId(tokenId1, userId)).thenReturn(0);

        // Act: 调用 service 方法
        boolean deleted = apiTokenService.deleteToken(tokenId1, userId);

        // Assert: 验证返回 false
        assertFalse(deleted);
        verify(apiTokenMapper, times(1)).deleteByIdAndUserId(tokenId1, userId);
    }
} 