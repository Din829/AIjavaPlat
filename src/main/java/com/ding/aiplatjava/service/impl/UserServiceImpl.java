package com.ding.aiplatjava.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PasswordEncoder passwordEncoder;

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
     * 创建新用户 (此方法可能主要由内部或测试使用)
     * 注意：此方法现在也对密码进行加密，与 registerUser 类似，但返回创建的对象。
     * @param user 用户对象（不含ID、创建时间和更新时间）
     * @return 创建后的用户对象（包含自动生成的ID和时间戳）
     */
    @Override
    @Transactional // 建议对写操作添加事务管理
    public User create(User user) {
        // 使用注入的 passwordEncoder 对明文密码进行加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
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
     * 注意：通常不建议在此方法中更新密码，应提供单独的密码修改接口。
     * @param user 需要更新的用户对象（必须包含ID）
     * @return 更新后的用户对象
     */
    @Override
    @Transactional // 建议对写操作添加事务管理
    public User update(User user) {
        // 自动更新"更新时间"字段为当前时间
        user.setUpdatedAt(LocalDateTime.now());
        // 如果传入的 user 对象包含密码，确保不直接更新（避免意外覆盖加密密码）
        // 可以在这里添加逻辑：如果 user.getPassword() 非空，则抛出异常或忽略密码字段
        user.setPassword(null); // 明确不在此方法中更新密码

        // 根据ID更新用户信息 (忽略密码)
        userMapper.updateById(user);
        // 可能需要重新查询以获取完整的更新后对象，取决于 updateById 的实现
        return userMapper.selectById(user.getId());
    }

    /**
     * 删除用户
     * @param id 要删除的用户ID
     */
    @Override
    @Transactional // 建议对写操作添加事务管理
    public void delete(Long id) {
        // 根据ID删除用户记录
        userMapper.deleteById(id);
    }

    /**
     * 注册新用户，处理密码加密。
     * @param user 包含用户名、邮箱和明文密码的用户对象。
     * @return 注册并保存到数据库后的用户对象。
     */
    @Override
    @Transactional // 确保注册操作的原子性
    public User registerUser(User user) {
        // 1. 使用 PasswordEncoder 对明文密码进行加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. 设置创建和更新时间
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        // 3. 调用 Mapper 将用户信息插入数据库
        userMapper.insert(user); // insert 方法应配置为返回生成的 ID 到 user 对象

        // 4. 返回包含完整信息（包括ID）的用户对象
        return user;
    }
}
