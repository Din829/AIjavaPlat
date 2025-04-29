package com.ding.aiplatjava.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ding.aiplatjava.entity.Prompt;

/**
 * Prompt 数据访问接口
 */
@Mapper
public interface PromptMapper {

    /**
     * 根据ID查询Prompt
     * @param id Prompt ID
     * @return Prompt实体，如果不存在返回null
     */
    Prompt selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询该用户的所有Prompt
     * @param userId 用户ID
     * @return Prompt列表
     */
    List<Prompt> selectByUserId(@Param("userId") Long userId);

    /**
     * 插入新的Prompt
     * @param prompt 要插入的Prompt实体 (应包含userId, title, content, category, createdAt, updatedAt)
     * @return 影响的行数
     */
    int insert(Prompt prompt);

    /**
     * 根据ID更新Prompt
     * @param prompt 要更新的Prompt实体 (应包含id, title, content, category, updatedAt; userId可选用于安全检查)
     * @return 影响的行数
     */
    int updateById(Prompt prompt);

    /**
     * 根据ID删除Prompt
     * @param id 要删除的Prompt ID
     * @param userId 执行删除操作的用户ID (可选用于安全检查)
     * @return 影响的行数
     */
    int deleteById(@Param("id") Long id, @Param("userId") Long userId);

} 