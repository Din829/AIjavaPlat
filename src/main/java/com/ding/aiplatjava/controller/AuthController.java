package com.ding.aiplatjava.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ding.aiplatjava.dto.AuthResponseDto;
import com.ding.aiplatjava.dto.LoginRequestDto;
import com.ding.aiplatjava.dto.RegisterRequestDto;
import com.ding.aiplatjava.dto.UserDto;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.service.UserService;
import com.ding.aiplatjava.util.JwtUtil;

import lombok.RequiredArgsConstructor;

/**
 * 处理用户认证（登录、注册）相关请求的控制器。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;//认证管理器
    private final JwtUtil jwtUtil;//JWT工具类
    private final UserService userService;//用户服务
    // PasswordEncoder 的注入和使用移到 UserService 中，以保持 Controller 的职责单一
    // private final PasswordEncoder passwordEncoder;

    /**
     * 处理用户登录请求。
     *
     * @param loginRequest 包含用户名和密码的登录请求 DTO。
     * @return 包含 JWT 访问令牌的响应实体。
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto loginRequest) {
        // 1. 使用 AuthenticationManager 进行认证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );

        // 2. 将认证信息设置到 SecurityContext (虽然对于无状态应用不是必须的，但有时有用)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. 从认证信息中获取 UserDetails (包含了用户名)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 4. 使用 JwtUtil 生成 JWT
        String jwt = jwtUtil.generateToken(userDetails);

        // 5. 返回包含 JWT 的响应
        return ResponseEntity.ok(new AuthResponseDto(jwt));
    }

    /**
     * 处理用户注册请求。
     *
     * @param registerRequest 包含注册信息的 DTO。
     * @return 注册成功或失败的响应实体。成功时返回包含新用户信息的 UserDto。
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto registerRequest) {
        // 基本校验：密码和确认密码是否一致
        if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Error: Passwords don't match!");
        }

        // 检查用户名或邮箱是否已存在 (可以在 UserService 中实现更健壮的检查)
        if (userService.findByUsername(registerRequest.getUsername()).isPresent()) {
             return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }
        if (userService.findByEmail(registerRequest.getEmail()).isPresent()) {
             return ResponseEntity.badRequest().body("Error: Email is already in use!");
        }

        // 创建 User 对象 (密码加密将在 UserService 中处理)
        User newUser = new User();
        newUser.setUsername(registerRequest.getUsername());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(registerRequest.getPassword()); // 传递原始密码给 Service

        try {
            // 调用 UserService 创建用户 (假设该方法返回创建后的 User 对象)
            User registeredUser = userService.registerUser(newUser);

            // 将返回的 User 实体转换为 UserDto (确保 UserDto 不含敏感信息)
            UserDto userDto = new UserDto();
            userDto.setId(registeredUser.getId());
            userDto.setUsername(registeredUser.getUsername());
            userDto.setEmail(registeredUser.getEmail());
            // 假设 UserDto 定义了这些字段，并且没有 password

            // 返回 201 Created 状态码和 UserDto 作为 JSON 响应体
            return ResponseEntity.status(HttpStatus.CREATED).body(userDto);
        } catch (Exception e) {
            // 处理可能的异常，例如数据库错误
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error registering user: " + e.getMessage());
        }
    }
} 