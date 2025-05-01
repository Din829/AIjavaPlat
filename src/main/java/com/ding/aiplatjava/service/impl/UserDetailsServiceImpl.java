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
     * @param username 要加载其数据的用户的用户名。
     * @return 包含用户核心信息的 UserDetails 对象 (不能为空)。
     * @throws UsernameNotFoundException 如果找不到具有给定用户名的用户。
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 使用 UserMapper 根据用户名从数据库查询用户实体
        User user = userMapper.selectByUsername(username);

        // 2. 如果用户不存在，抛出 UsernameNotFoundException，Spring Security 会处理此异常
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }

        // 3. 如果用户存在，将其转换为 Spring Security 理解的 UserDetails 对象
        //    - 第一个参数: 用户名
        //    - 第二个参数: 数据库中存储的加密密码
        //    - 第三个参数: 用户的权限列表 (GrantedAuthority)。暂时使用空列表，后续可扩展角色/权限。
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.emptyList() // 暂时没有角色/权限
                // 如果有角色/权限，可以像这样构建:
                // AuthorityUtils.commaSeparatedStringToAuthorityList("ROLE_USER,ROLE_ADMIN")
        );
    }
} 