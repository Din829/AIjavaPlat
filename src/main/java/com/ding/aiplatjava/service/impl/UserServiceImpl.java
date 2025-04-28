package com.ding.aiplatjava.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.mapper.UserMapper;
import com.ding.aiplatjava.service.UserService;

import lombok.RequiredArgsConstructor;

/**
 * 用户服务实现类
 * 实现了UserService接口，提供用户相关的业务逻辑处理
 */
@Service // 标记为Spring的服务组件，会被自动扫描并注册到容器中
@RequiredArgsConstructor // Lombok注解，自动生成包含所有final字段的构造函数，实现依赖注入
public class UserServiceImpl implements UserService {

    // 通过构造函数注入UserMapper，用于数据库操作
    private final UserMapper userMapper;

    /**
     * 根据ID查找用户
     * @param id 用户ID
     * @return 包装在Optional中的用户对象，如果不存在则为空
     */
    @Override
    public Optional<User> findById(Long id) {
        // 调用MyBatis的selectById方法，并将结果包装在Optional中
        return Optional.ofNullable(userMapper.selectById(id));
    }

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return 包装在Optional中的用户对象，如果不存在则为空
     */
    @Override
    public Optional<User> findByUsername(String username) {
        // 调用自定义的selectByUsername方法，实现按用户名精确查询
        return Optional.ofNullable(userMapper.selectByUsername(username));
    }

    /**
     * 根据电子邮件查找用户
     * @param email 电子邮件
     * @return 包装在Optional中的用户对象，如果不存在则为空
     */
    @Override
    public Optional<User> findByEmail(String email) {
        // 调用自定义的selectByEmail方法，实现按邮箱精确查询
        return Optional.ofNullable(userMapper.selectByEmail(email));
    }

    /**
     * 查找所有用户
     * @return 用户列表
     */
    @Override
    public List<User> findAll() {
        // 调用selectList方法查询所有记录
        return userMapper.selectList();
    }

    /**
     * 创建新用户
     * @param user 用户对象（不含ID、创建时间和更新时间）
     * @return 创建后的用户对象（包含自动生成的ID和时间戳）
     */
    @Override
    public User create(User user) {
        // 设置创建和更新时间为当前时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // 执行插入操作，MyBatis会自动设置生成的ID到user对象
        userMapper.insert(user);
        return user; // 返回包含ID的用户对象
    }

    /**
     * 更新用户信息
     * @param user 需要更新的用户对象（必须包含ID）
     * @return 更新后的用户对象
     */
    @Override
    public User update(User user) {
        // 自动更新"更新时间"字段为当前时间
        user.setUpdatedAt(LocalDateTime.now());

        // 根据ID更新用户信息
        userMapper.updateById(user);
        return user;
    }

    /**
     * 删除用户
     * @param id 要删除的用户ID
     */
    @Override
    public void delete(Long id) {
        // 根据ID删除用户记录
        userMapper.deleteById(id);
    }
}
