package com.ding.aiplatjava.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

import com.ding.aiplatjava.security.JwtAuthFilter;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Spring Security 配置类
 * 启用Web安全并定义安全规则、认证机制等。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // 使用 Lombok 自动生成构造函数注入依赖
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter; // 注入 JWT 认证过滤器
    private final UserDetailsService userDetailsService; // 注入用户详情服务
    private final ObjectMapper objectMapper; // Added ObjectMapper injection

    /**
     * 定义密码编码器 Bean。
     * 使用 BCrypt 算法。
     * @return PasswordEncoder 实例
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 定义认证提供者 Bean。
     * 使用 DaoAuthenticationProvider，这是 Spring Security 提供的标准实现。
     * 它会使用 UserDetailsService 加载用户信息，并使用 PasswordEncoder 比较密码。
     * @return AuthenticationProvider 实例
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // 设置用户详情服务
        authProvider.setPasswordEncoder(passwordEncoder()); // 设置密码编码器
        return authProvider;
    }

    /**
     * 定义认证管理器 Bean。
     * AuthenticationManager 是 Spring Security 认证的核心接口。
     * 我们从 AuthenticationConfiguration 中获取它，这是推荐的方式。
     * 这个 Bean 将在 AuthController 中用于处理登录请求。
     * @param config AuthenticationConfiguration 对象
     * @return AuthenticationManager 实例
     * @throws Exception 如果获取 AuthenticationManager 失败
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 定义 CORS 配置源 Bean。
     * 这个 Bean 提供了跨域请求的配置信息。
     * @return CorsConfigurationSource 实例
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许来自前端开发服务器的请求
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        // 允许所有常见的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        configuration.setAllowedHeaders(List.of("*"));
        // 允许发送凭证 (如 cookies)
        configuration.setAllowCredentials(true);
        // 设置预检请求的最大缓存时间
        configuration.setMaxAge(3600L); // 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用此配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 配置安全过滤器链。
     * 定义了哪些请求需要认证，哪些可以公开访问，以及使用的认证机制等。
     *
     * @param http HttpSecurity 对象，用于构建安全配置。
     * @return 配置好的 SecurityFilterChain。
     * @throws Exception 如果配置过程中发生错误。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 添加 CORS 配置
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 使用我们定义的 Bean
            // 1. 禁用 CSRF 保护 (因为我们使用无状态 JWT)
            .csrf(AbstractHttpConfigurer::disable)
            // 2. 配置请求授权规则 (Authorization)
            .authorizeHttpRequests(authorize -> authorize
                // 指定路径完全公开访问，不需要认证
                .requestMatchers("/api/auth/**").permitAll() // 允许所有对 /api/auth/ 下路径的请求 (用于登录、注册等)
                // .requestMatchers("/api/test/**").permitAll() // 移除测试接口的permitAll
                // 其他所有未明确匹配的请求都需要认证
                .anyRequest().authenticated()
            )
            // 3. 配置会话管理 (Session Management) 为无状态
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // 2.1 Configure custom authentication entry point and access denied handler
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(objectMapper.writeValueAsString(
                        Map.of("timestamp", LocalDateTime.now().toString(),
                               "status", HttpServletResponse.SC_UNAUTHORIZED,
                               "error", "Unauthorized",
                               "message", authException.getMessage(),
                               "path", request.getRequestURI())
                    ));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(objectMapper.writeValueAsString(
                        Map.of("timestamp", LocalDateTime.now().toString(),
                               "status", HttpServletResponse.SC_FORBIDDEN,
                               "error", "Forbidden",
                               "message", accessDeniedException.getMessage(),
                               "path", request.getRequestURI())
                    ));
                })
            )
            // 4. 配置认证提供者
            .authenticationProvider(authenticationProvider()) // 使用上面定义的 AuthenticationProvider
            // 5. 添加 JWT 认证过滤器
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // 构建并返回过滤器链
        return http.build();
    }
}