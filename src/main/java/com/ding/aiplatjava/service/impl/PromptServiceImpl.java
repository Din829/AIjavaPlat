package com.ding.aiplatjava.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 引入事务注解

import com.ding.aiplatjava.entity.Prompt;
import com.ding.aiplatjava.mapper.PromptMapper;
import com.ding.aiplatjava.service.PromptService;

/**
 * Prompt 服务实现类
 * 提供了对 Prompt 实体的业务逻辑操作实现。
 */
@Service
public class PromptServiceImpl implements PromptService {

    @Autowired
    private PromptMapper promptMapper;

    /**
     * 根据 Prompt ID 获取 Prompt 详情。
     * 同时会校验该 Prompt 是否属于指定的用户 ID。
     *
     * @param id     要查询的 Prompt ID。
     * @param userId 发起请求的用户 ID，用于权限校验。
     * @return 如果找到且用户匹配，则返回 Prompt 实体；否则返回 null。
     *         后续可考虑抛出自定义异常（如 NotFoundException 或 AccessDeniedException）以提供更明确的错误信息。
     */
    @Override
    public Prompt getPromptById(Long id, Long userId) {
        Prompt prompt = promptMapper.selectById(id);
        // 校验 Prompt 是否存在，以及其 userId 是否与请求的用户 ID 匹配
        if (prompt != null && Objects.equals(prompt.getUserId(), userId)) {
            return prompt;
        }
        return null;
    }

    /**
     * 获取指定用户 ID 创建的所有 Prompts。
     * 结果会按照更新时间降序排列。
     *
     * @param userId 要查询的用户 ID。
     * @return 包含该用户所有 Prompts 的列表，如果用户没有任何 Prompt，则返回空列表。
     */
    @Override
    public List<Prompt> getPromptsByUserId(Long userId) {
        return promptMapper.selectByUserId(userId);
    }

    /**
     * 创建一个新的 Prompt。
     * 会自动设置创建时间和更新时间，并关联到指定的用户 ID。
     * 此操作在一个事务中执行。
     *
     * @param prompt 包含新 Prompt 信息的实体（通常由 Controller 传入，包含 title, content, category）。
     * @param userId 创建该 Prompt 的用户 ID。
     * @return 创建成功后，包含完整信息（包括数据库生成的 ID、设置的 userId、创建和更新时间）的 Prompt 实体。
     */
    @Override
    @Transactional // 保证插入操作的原子性
    public Prompt createPrompt(Prompt prompt, Long userId) {
        // 强制设置传入的 prompt 实体的 userId 为当前操作用户 ID
        prompt.setUserId(userId);
        LocalDateTime now = LocalDateTime.now();
        // 设置创建和更新时间
        prompt.setCreatedAt(now);
        prompt.setUpdatedAt(now);
        // 调用 Mapper 执行数据库插入
        promptMapper.insert(prompt);
        // MyBatis 配置了 useGeneratedKeys="true" keyProperty="id"，
        // 因此插入后，传入的 prompt 对象会被自动填充上数据库生成的 ID。
        return prompt;
    }

    /**
     * 更新一个已存在的 Prompt。
     * 只有 Prompt 的创建者才能更新自己的 Prompt。
     * 只允许更新 title, content, category 字段，并自动更新 updatedAt 字段。
     * 此操作在一个事务中执行。
     *
     * @param id            要更新的 Prompt 的 ID。
     * @param promptDetails 包含要更新的字段信息 (title, content, category) 的 Prompt 实体。
     * @param userId        发起更新请求的用户 ID，用于权限校验。
     * @return 如果更新成功，返回更新后的 Prompt 完整实体；如果 Prompt 不存在或用户无权更新，则返回 null。
     */
    @Override
    @Transactional // 保证查询和更新操作的原子性
    public Prompt updatePrompt(Long id, Prompt promptDetails, Long userId) {
        // 1. 先根据 ID 从数据库获取当前的 Prompt 实体
        Prompt existingPrompt = promptMapper.selectById(id);
        
        // 2. 校验 Prompt 是否存在，以及其 userId 是否与请求的用户 ID 匹配
        if (existingPrompt != null && Objects.equals(existingPrompt.getUserId(), userId)) {
            // 3. 使用传入的 promptDetails 更新 existingPrompt 中允许修改的字段
            existingPrompt.setTitle(promptDetails.getTitle());
            existingPrompt.setContent(promptDetails.getContent());
            existingPrompt.setCategory(promptDetails.getCategory());
            // 更新"更新时间"
            existingPrompt.setUpdatedAt(LocalDateTime.now());
            
            // 4. (可选，增强健壮性) 设置 userId 用于 Mapper 层面的安全检查 (updateById SQL 中包含 AND user_id = #{userId})
            // 尽管业务层已校验，但 Mapper 层再校验一次可以防止逻辑错误。
            existingPrompt.setUserId(userId);

            // 5. 调用 Mapper 执行数据库更新
            int updatedRows = promptMapper.updateById(existingPrompt);
            
            // 6. 检查更新是否成功 (影响行数大于0)
            if (updatedRows > 0) {
                 // 7. 更新成功，从数据库重新获取最新的 Prompt 数据并返回
                 //    (确保返回的数据包含数据库实际更新的时间戳等)
                return promptMapper.selectById(id);
            } else {
                // 更新影响行数为0，可能意味着并发冲突或其他问题，此处返回 null
                // 后续可考虑抛出异常，例如 OptimisticLockingFailureException
                return null;
            }
        }
        // Prompt 不存在或用户无权访问，返回 null
        return null;
    }

    /**
     * 根据 ID 删除一个 Prompt。
     * 只有 Prompt 的创建者才能删除自己的 Prompt。
     * 此操作在一个事务中执行。
     *
     * @param id     要删除的 Prompt ID。
     * @param userId 发起删除请求的用户 ID，用于权限校验。
     * @return 如果删除成功（找到了对应 ID 和用户 ID 的记录并删除），返回 true；否则返回 false。
     */
    @Override
    @Transactional // 事务，保证删除操作的原子性
    public boolean deletePrompt(Long id, Long userId) {
        //直接调用 Mapper 的 deleteById 方法，该方法在 SQL 层面包含了对 userId 的校验
        int deletedRows = promptMapper.deleteById(id, userId);
        // 如果影响行数大于 0，说明删除成功
        return deletedRows > 0;
    }
} 