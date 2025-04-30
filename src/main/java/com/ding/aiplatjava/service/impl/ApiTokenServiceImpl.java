package com.ding.aiplatjava.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ding.aiplatjava.entity.ApiToken;
import com.ding.aiplatjava.mapper.ApiTokenMapper;
import com.ding.aiplatjava.service.ApiTokenService;
import com.ding.aiplatjava.util.EncryptionUtil;

import lombok.RequiredArgsConstructor;

/**
 * API Token 服务实现类
 */
@Service
@RequiredArgsConstructor // 使用 Lombok 自动生成构造函数注入 final 字段
public class ApiTokenServiceImpl implements ApiTokenService {

    private final ApiTokenMapper apiTokenMapper;
    private final EncryptionUtil encryptionUtil;

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
                return encryptionUtil.decrypt(apiToken.getTokenValue());
            } catch (RuntimeException e) {
                // 处理解密失败的情况，例如记录日志
                // TODO: 替换为更健壮的日志记录
                System.err.println("Failed to decrypt token with ID: " + tokenId + " for user: " + userId + "; Error: " + e.getMessage());
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
} 