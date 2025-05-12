package com.ding.aiplatjava.service.impl;

import java.util.Collections;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.mapper.UserMapper;

import lombok.RequiredArgsConstructor; // Import for Collections.emptyList()

/**
 * UserDetailsService 的实现类。
 * 负责从数据库加载用户特定数据以进行身份验证和授权。
 */
@Service
@RequiredArgsConstructor // 使用 Lombok 自动生成包含 final 字段的构造函数进行依赖注入
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper; // 注入 UserMapper 以访问用户数据

    /**
     * 根据用户名加载用户核心信息。
     *
     * @param usernameOrEmail 要加载其数据的用户的用户名或邮箱。
     * @return 包含用户核心信息的 UserDetails 对象 (不能为空)。
     * @throws UsernameNotFoundException 如果找不到具有给定用户名或邮箱的用户。
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // 1. 尝试根据传入的参数查找用户，优先判断是否为邮箱
        User user = null;
        if (usernameOrEmail != null && usernameOrEmail.contains("@")) {
            // 如果包含@符号，认为是邮箱，调用selectByEmail
            user = userMapper.selectByEmail(usernameOrEmail);
        } else {
            // 否则，认为是用户名，调用selectByUsername
            user = userMapper.selectByUsername(usernameOrEmail);
        }

        // 2. 如果用户不存在，抛出 UsernameNotFoundException
        if (user == null) {
            throw new UsernameNotFoundException("User not found with identifier: " + usernameOrEmail);
        }

        // 3. 如果用户存在，将其转换为 Spring Security 理解的 UserDetails 对象
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), // 使用数据库中的用户名作为 UserDetails 的 username
                user.getPassword(),
                Collections.emptyList() // 暂时没有角色/权限
        );
    }
} 