package com.ding.aiplatjava.security;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.ding.aiplatjava.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link JwtAuthFilter} 的单元测试。
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    private UserDetails testUserDetails;
    private final String testJwt = "validTestJwt";
    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        // 在每个测试之前清除上下文
        SecurityContextHolder.clearContext();
        testUserDetails = new User(testUsername, "password", Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        // 在每个测试之后清除上下文
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldCallNextFilter_whenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        // Arrange: 请求没有 Authorization 头
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act: 应用过滤器
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert: SecurityContext 应为空，且应调用下一个过滤器
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        // 验证没有与 jwtUtil 或 userDetailsService 的交互发生
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_shouldCallNextFilter_whenAuthorizationHeaderDoesNotStartWithBearer() throws ServletException, IOException {
        // Arrange: 请求带有无效的 Authorization 头
        when(request.getHeader("Authorization")).thenReturn("InvalidToken");

        // Act: 应用过滤器
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert: SecurityContext 应为空，且应调用下一个过滤器
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilterInternal_shouldSetAuthentication_whenTokenIsValid() throws ServletException, IOException {
        // Arrange: 有效的 Authorization 头和有效的令牌处理
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testJwt);
        when(jwtUtil.extractUsername(testJwt)).thenReturn(testUsername);
        // 确保上下文最初为 null
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(testUserDetails);
        when(jwtUtil.isTokenValid(testJwt, testUserDetails)).thenReturn(true);

        // Act: 应用过滤器
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert: SecurityContext 应被填充，且应调用下一个过滤器
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(testUsername, SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(testUserDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).extractUsername(testJwt);
        verify(userDetailsService).loadUserByUsername(testUsername);
        verify(jwtUtil).isTokenValid(testJwt, testUserDetails);
    }

    @Test
    void doFilterInternal_shouldCallNextFilter_whenTokenIsInvalid() throws ServletException, IOException {
        // Arrange: 有效的 Authorization 头，但令牌验证失败
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testJwt);
        when(jwtUtil.extractUsername(testJwt)).thenReturn(testUsername);
        assertNull(SecurityContextHolder.getContext().getAuthentication()); // 上下文开始为空
        when(userDetailsService.loadUserByUsername(testUsername)).thenReturn(testUserDetails);
        when(jwtUtil.isTokenValid(testJwt, testUserDetails)).thenReturn(false); // 令牌无效

        // Act: 应用过滤器
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert: SecurityContext 应保持为空，且应调用下一个过滤器
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).extractUsername(testJwt);
        verify(userDetailsService).loadUserByUsername(testUsername);
        verify(jwtUtil).isTokenValid(testJwt, testUserDetails); // isTokenValid 被调用了
    }

     @Test
    void doFilterInternal_shouldCallNextFilter_whenUsernameIsNullButContextAlreadySet() throws ServletException, IOException {
         // Arrange: 模拟上下文已被设置的场景（例如，被另一个过滤器设置）
         // 我们不会在这里实际设置它，但会确保条件导致上下文检查
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testJwt);
        when(jwtUtil.extractUsername(testJwt)).thenReturn(testUsername);
         // 手动设置上下文以模拟它已被预先填充
        SecurityContextHolder.getContext().setAuthentication(mock(org.springframework.security.core.Authentication.class));

        // Act: 应用过滤器
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

         // Assert: 调用了过滤器链，但不应调用 UserDetailsService 和 isTokenValid
         // 因为上下文已被填充。
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).extractUsername(testJwt); // 提取了用户名
         // 关键是：如果上下文已设置，这些不应被调用
        verifyNoInteractions(userDetailsService);
        verify(jwtUtil, never()).isTokenValid(anyString(), any(UserDetails.class));
    }


    @Test
    void doFilterInternal_shouldCallNextFilter_whenJwtUtilThrowsException() throws ServletException, IOException {
        // Arrange: jwtUtil 在提取用户名期间抛出异常
        when(request.getHeader("Authorization")).thenReturn("Bearer " + testJwt);
        when(jwtUtil.extractUsername(testJwt)).thenThrow(new RuntimeException("JWT parsing error"));

        // Act: 应用过滤器
        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        // Assert: SecurityContext 应为空，且应调用下一个过滤器
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).extractUsername(testJwt);
        // 如果提取失败，不应调用 UserDetailsService 和 isTokenValid
        verifyNoInteractions(userDetailsService);
        verify(jwtUtil, never()).isTokenValid(anyString(), any(UserDetails.class));
    }
} 