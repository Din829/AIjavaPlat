package com.ding.aiplatjava.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ding.aiplatjava.entity.ApiToken;
import com.ding.aiplatjava.mapper.ApiTokenMapper;
import com.ding.aiplatjava.service.ApiTokenService;
import com.ding.aiplatjava.util.EncryptionUtil;
import com.ding.aiplatjava.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * ApiTokenService 的实现类。
 * 负责处理与 API Token 相关的业务逻辑，包括加密存储。
 */
@Service
@RequiredArgsConstructor // 使用 Lombok 自动生成构造函数
public class ApiTokenServiceImpl implements ApiTokenService {

    private final ApiTokenMapper apiTokenMapper;
    private final EncryptionUtil encryptionUtil; // 注入 EncryptionUtil 实例
    private static final Logger log = LoggerFactory.getLogger(ApiTokenServiceImpl.class);
    
    /**
     * 创建 API Token，包含加密逻辑。
     */
    @Override
    @Transactional
    public ApiToken createToken(ApiToken apiToken, Long userId) {
        // 1. 使用加密工具加密明文 tokenValue
        String encryptedToken = encryptionUtil.encrypt(apiToken.getTokenValue());
        
        // 2. 准备要存入数据库的实体
        ApiToken tokenToSave = new ApiToken();
        tokenToSave.setUserId(userId);
        tokenToSave.setProvider(apiToken.getProvider());
        tokenToSave.setTokenValue(encryptedToken); // 存储加密后的值
        LocalDateTime now = LocalDateTime.now();
        tokenToSave.setCreatedAt(now);
        tokenToSave.setUpdatedAt(now);

        // 3. 调用 Mapper 插入数据库
        apiTokenMapper.insert(tokenToSave);
        // insert 方法配置了 useGeneratedKeys="true" keyProperty="id"，
        // 所以执行后 tokenToSave 对象会包含数据库生成的ID

        // 4. 返回创建后的实体 (tokenValue 仍然是加密的)
        return tokenToSave;
    }

    /**
     * 根据用户 ID 获取 Token 列表。
     */
    @Override
    public List<ApiToken> getTokensByUserId(Long userId) {
        // 调用 Mapper 获取列表
        return apiTokenMapper.selectByUserId(userId);
    }

    /**
     * 获取解密后的 Token 值，包含权限校验。
     */
    @Override
    public String getDecryptedTokenValue(Long tokenId, Long userId) {
        // 调用 Mapper 获取 Token 实体
        ApiToken apiToken = apiTokenMapper.selectById(tokenId);

        // 1. 检查 Token 是否存在且属于当前用户
        if (apiToken != null && Objects.equals(apiToken.getUserId(), userId)) {
            // 2. 解密 tokenValue
            try {
                // 使用注入的 encryptionUtil 实例调用非静态方法
                String decryptedValue = encryptionUtil.decrypt(apiToken.getTokenValue());
                log.info("Successfully decrypted token for user ID: {} and provider: {}", userId, apiToken.getProvider());
                return decryptedValue;
            } catch (Exception e) {
                // 处理解密失败的情况，例如记录日志
                log.error("Failed to decrypt token with ID: " + tokenId + " for user: " + userId + "; Error: " + e.getMessage());
                return null;
            }
        } else {
            // Token 不存在或用户无权访问
            return null;
        }
    }

    /**
     * 删除 Token，包含权限校验。
     */
    @Override
    @Transactional
    public boolean deleteToken(Long tokenId, Long userId) {
        // 调用 Mapper 删除，SQL 中已包含 userId 校验
        int deletedRows = apiTokenMapper.deleteByIdAndUserId(tokenId, userId);
        return deletedRows > 0;
    }

    /**
     * 根据用户 ID 和提供商获取解密后的 Token 值。
     *
     * @param userId 用户 ID。
     * @param provider 提供商名称 (例如 "openai")。
     * @return 解密后的 API Token 字符串。
     * @throws ResourceNotFoundException 如果未找到指定用户或提供商的 Token。
     * @throws RuntimeException 如果解密失败。
     */
    @Override
    public String getDecryptedTokenValueByProvider(Long userId, String provider) {
        log.debug("Attempting to get decrypted token for user ID: {} and provider: {}", userId, provider);
        // 1. 根据 userId 和 provider 查询 Token 实体
        ApiToken apiToken = apiTokenMapper.selectByUserIdAndProvider(userId, provider);

        // 2. 检查 Token 是否存在
        if (apiToken == null) {
            log.warn("No API token found for user ID: {} and provider: {}", userId, provider);
            throw new ResourceNotFoundException("未找到用户 " + userId + " 的 " + provider + " API Token");
        }

        // 3. 解密 Token 值
        try {
            // 使用注入的 encryptionUtil 实例调用非静态方法
            String decryptedValue = encryptionUtil.decrypt(apiToken.getTokenValue());
            log.info("Successfully decrypted token for user ID: {} and provider: {}", userId, provider);
            return decryptedValue;
        } catch (Exception e) {
            log.error("Failed to decrypt token with ID: {} for user: {} and provider: {}", apiToken.getId(), userId, provider, e);
            // 考虑是否需要更具体的异常类型或处理方式
            throw new RuntimeException("Token 解密失败", e);
        }
    }
} 