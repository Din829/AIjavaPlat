package com.ding.aiplatjava.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ding.aiplatjava.entity.ApiToken;

/**
 * API Token 数据访问接口
 */
@Mapper
public interface ApiTokenMapper {

    /**
     * 根据ID查询 ApiToken。
     * @param id Token ID。
     * @return ApiToken 实体，如果不存在则返回 null。
     */
    ApiToken selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询该用户的所有 ApiTokens。
     * @param userId 用户 ID。
     * @return ApiToken 列表。
     */
    List<ApiToken> selectByUserId(@Param("userId") Long userId);

    /**
     * 插入新的 ApiToken。
     * @param apiToken 要插入的 ApiToken 实体 (应包含 userId, provider, 加密的 tokenValue, createdAt, updatedAt)。
     * @return 影响的行数。
     */
    int insert(ApiToken apiToken);

    // 注意：通常不直接更新 Token 值本身，而是删除旧的，创建新的。
    // 如果需要更新其他字段（例如 provider），可以添加 update 方法。

    /**
     * 根据 Token ID 和用户 ID 删除 ApiToken。
     * 添加 userId 是为了确保用户只能删除自己的 Token。
     * @param id 要删除的 Token ID。
     * @param userId 执行删除操作的用户 ID。
     * @return 影响的行数。
     */
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

} 