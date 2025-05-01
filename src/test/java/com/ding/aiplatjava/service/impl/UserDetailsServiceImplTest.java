package com.ding.aiplatjava.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.mapper.UserMapper;

/**
 * {@link UserDetailsServiceImpl} 的单元测试。
 */
@ExtendWith(MockitoExtension.class) // 启用 Mockito 支持
class UserDetailsServiceImplTest {

    @Mock // 创建 UserMapper 的模拟实例
    private UserMapper userMapper;

    @InjectMocks // 创建 UserDetailsServiceImpl 的实例并将模拟对象注入其中
    private UserDetailsServiceImpl userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword"); // 假设密码已预先编码
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails_whenUserExists() {
        // Arrange: 配置模拟 UserMapper 在调用 selectByUsername 时返回 testUser
        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);

        // Act: 调用被测试的方法
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert: 验证返回的 UserDetails 与 testUser 的数据匹配
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().isEmpty()); // 假设目前没有权限
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());

        // 验证 userMapper.selectByUsername 使用 "testuser" 被精确调用了一次
        verify(userMapper).selectByUsername("testuser");
    }

    @Test
    void loadUserByUsername_shouldThrowUsernameNotFoundException_whenUserDoesNotExist() {
        // Arrange: 配置模拟 UserMapper 对任何用户名都返回 null
        when(userMapper.selectByUsername(anyString())).thenReturn(null);

        // Act & Assert: 调用方法并断言抛出了 UsernameNotFoundException
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("nonexistentuser");
        });

        // 验证 userMapper.selectByUsername 被调用了
        verify(userMapper).selectByUsername("nonexistentuser");
    }
} 