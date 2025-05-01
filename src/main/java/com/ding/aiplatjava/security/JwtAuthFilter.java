package com.ding.aiplatjava.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ding.aiplatjava.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * JWT 认证过滤器。
 * 继承 OncePerRequestFilter 确保每个请求只执行一次此过滤器。
 * 负责从请求中提取、验证 JWT，并在验证成功后设置 Spring Security 的认证上下文。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; // 用于处理 JWT 的工具类
    private final UserDetailsService userDetailsService; // 用于加载用户信息的服务

    /**
     * 过滤器的核心逻辑。
     *
     * @param request     HttpServletRequest 对象
     * @param response    HttpServletResponse 对象
     * @param filterChain FilterChain 对象，用于将请求传递给下一个过滤器
     * @throws ServletException 如果发生 Servlet 相关异常
     * @throws IOException      如果发生 I/O 异常
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 从请求头中获取 Authorization Header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 2. 检查 Authorization Header 是否存在且以 "Bearer " 开头
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 如果没有有效的 Header，则直接调用下一个过滤器并返回
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 提取 JWT (去除 "Bearer " 前缀)
        jwt = authHeader.substring(7);

        try {
            // 4. 从 JWT 中提取用户名 (Subject)
            username = jwtUtil.extractUsername(jwt);

            // 5. 检查用户名是否已提取且当前 SecurityContext 中没有认证信息
            //    (避免重复设置认证信息)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 6. 根据用户名加载 UserDetails
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // 7. 验证 JWT 是否有效 (用户名匹配且未过期)
                if (jwtUtil.isTokenValid(jwt, userDetails)) {
                    // 8. 如果 Token 有效，创建一个 Spring Security 的 Authentication 对象
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,       // Principal (通常是 UserDetails 对象)
                            null,              // Credentials (对于 JWT 认证，通常是 null)
                            userDetails.getAuthorities() // 用户的权限列表
                    );
                    // 9. 设置认证对象的详细信息 (通常是 WebAuthenticationDetails)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)//构建认证信息
                    );
                    // 10. 更新 SecurityContextHolder 中的认证信息
                    SecurityContextHolder.getContext().setAuthentication(authToken);//标记为已认证
                }
            }
        } catch (Exception e) {
            // 如果 JWT 解析或验证失败 (例如，过期、签名无效)，允许请求继续，
            // 但不设置认证信息。后续的安全规则将拒绝访问受保护的资源。
            // 可以考虑在这里记录日志。
            logger.warn("JWT validation failed: " + e.getMessage());
        }

        // 11. 调用过滤器链中的下一个过滤器
        filterChain.doFilter(request, response);//放行
    }
} 