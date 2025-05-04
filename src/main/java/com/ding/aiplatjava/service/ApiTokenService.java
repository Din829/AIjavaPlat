package com.ding.aiplatjava.service;

import java.util.List;

import com.ding.aiplatjava.entity.ApiToken;

/**
 * API Token 服务接口
 * 定义 API Token 相关的业务逻辑操作，包括加密存储和安全访问。
 */
public interface ApiTokenService {

    /**
     * 为指定用户创建一个新的 API Token。
     * Token 值在存储前会被加密。
     *
     * @param apiToken 包含 Token 提供商 (provider) 和明文 Token 值 (tokenValue) 的实体。
     * @param userId   要关联的用户 ID。
     * @return 创建成功后包含加密 Token 和其他信息的 ApiToken 实体。
     */
    ApiToken createToken(ApiToken apiToken, Long userId);

    /**
     * 获取指定用户的所有 API Tokens。
     * 返回的 Token 列表中的 tokenValue 字段保持加密状态。
     *
     * @param userId 用户 ID。
     * @return 该用户的所有 ApiToken 列表。
     */
    List<ApiToken> getTokensByUserId(Long userId);

    /**
     * 根据 Token ID 获取解密后的 Token 值。
     * !! 这是一个敏感操作，需要严格校验用户权限 !!
     * 仅在确实需要使用原始 Token 值与外部服务交互时调用。
     *
     * @param tokenId 要解密的 Token ID。
     * @param userId  当前用户 ID，用于校验该用户是否有权访问此 Token。
     * @return 解密后的明文 Token 值，如果 Token 不存在或用户无权访问则返回 null 或抛出异常。
     */
    String getDecryptedTokenValue(Long tokenId, Long userId);

    /**
     * 删除指定 ID 的 API Token。
     * 需要校验用户权限，确保用户只能删除自己的 Token。
     *
     * @param tokenId 要删除的 Token ID。
     * @param userId  当前用户 ID，用于校验权限。
     * @return 如果删除成功返回 true，否则返回 false (例如 Token 不存在或无权删除)。
     */
    boolean deleteToken(Long tokenId, Long userId);

    /**
     * 根据用户 ID 和提供商获取解密后的 Token 值。
     *
     * @param userId 用户 ID。
     * @param provider 提供商名称 (例如 "openai")。
     * @return 解密后的 API Token 字符串。
     * @throws ResourceNotFoundException 如果未找到指定用户或提供商的 Token。
     * @throws RuntimeException 如果解密失败。
     */
    String getDecryptedTokenValueByProvider(Long userId, String provider);

} 