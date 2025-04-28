package com.ding.aiplatjava.service;

import com.ding.aiplatjava.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 * 定义了用户相关的业务操作方法，由具体的实现类提供实现
 * 使用Optional包装返回值以处理可能的空值情况
 */
public interface UserService {

    /**
     * 根据ID查询用户
     * 用于获取特定ID的用户详细信息
     *
     * @param id 用户ID，数据库主键
     * @return 包装在Optional中的用户对象，如果不存在则为空
     */
    Optional<User> findById(Long id);

    /**
     * 根据用户名查询用户
     * 用于用户登录验证或检查用户名是否已存在
     *
     * @param username 用户名，唯一标识
     * @return 包装在Optional中的用户对象，如果不存在则为空
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据电子邮件查询用户
     * 用于邮箱登录验证或找回密码功能
     *
     * @param email 电子邮件，唯一标识
     * @return 包装在Optional中的用户对象，如果不存在则为空
     */
    Optional<User> findByEmail(String email);

    /**
     * 查询所有用户
     * 用于管理员查看用户列表等功能
     *
     * @return 所有用户的列表，如果没有用户则返回空列表
     */
    List<User> findAll();

    /**
     * 创建新用户
     * 用于用户注册功能
     *
     * @param user 用户对象，包含用户名、邮箱、密码等信息，不需要包含ID
     * @return 创建后的用户对象，包含自动生成的ID和时间戳
     */
    User create(User user);

    /**
     * 更新用户信息
     * 用于用户修改个人信息、管理员修改用户信息等功能
     *
     * @param user 用户对象，必须包含ID和需要更新的字段
     * @return 更新后的用户对象
     */
    User update(User user);

    /**
     * 删除用户
     * 用于用户注销账号或管理员删除用户等功能
     *
     * @param id 要删除的用户ID
     */
    void delete(Long id);
}
