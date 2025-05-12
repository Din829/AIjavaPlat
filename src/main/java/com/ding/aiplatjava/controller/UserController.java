package com.ding.aiplatjava.controller;

import com.ding.aiplatjava.dto.UserDto;
import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.exception.ResourceNotFoundException;
import com.ding.aiplatjava.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户控制器
 * 处理与用户相关的HTTP请求，提供RESTful API接口
 *
 * 主要功能：
 * - 获取用户列表
 * - 获取特定用户详情
 * - (待实现) 创建新用户
 * - (待实现) 更新用户信息
 * - (待实现) 删除用户
 */
@RestController // 标记为REST控制器，返回的对象会自动转换为JSON
@RequestMapping("/api/users") // 设置基础URL路径
@RequiredArgsConstructor // Lombok注解，自动生成包含所有final字段的构造函数，实现依赖注入
public class UserController {

    // 通过构造函数注入UserService，用于处理业务逻辑
    private final UserService userService;

    /**
     * 获取所有用户
     *
     * HTTP请求: GET /api/users
     *
     * @return 包含所有用户DTO的ResponseEntity，状态码200
     */
    @GetMapping // 映射HTTP GET请求
    public ResponseEntity<List<UserDto>> getAllUsers() {
        // 调用服务层获取所有用户
        List<User> userList = userService.findAll();

        // 创建一个新的用户DTO列表
        List<UserDto> userDtoList = new ArrayList<>();

        // 遍历用户列表，将每个用户实体转换为DTO
        for (User user : userList) {
            UserDto userDto = convertToDto(user);
            userDtoList.add(userDto);
        }

        // 返回200 OK状态码和用户DTO列表
        return ResponseEntity.ok(userDtoList);
    }

    /**
     * 根据ID获取用户
     *
     * HTTP请求: GET /api/users/{id}
     *
     * @param id 用户ID，从URL路径中获取
     * @return 包含用户DTO的ResponseEntity，状态码200
     * @throws ResourceNotFoundException 如果指定ID的用户不存在
     */
    @GetMapping("/{id}") // 映射带路径变量的HTTP GET请求
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        // 调用服务层查找用户
        java.util.Optional<User> userOptional = userService.findById(id);

        // 检查用户是否存在
        if (!userOptional.isPresent()) {
            // 如果用户不存在，抛出资源未找到异常
            throw new ResourceNotFoundException("User", "id", id);
        }

        // 获取用户对象
        User user = userOptional.get();

        // 将User实体转换为UserDto
        UserDto userDto = convertToDto(user);

        // 返回200 OK状态码和用户DTO
        return ResponseEntity.ok(userDto);
    }

    /**
     * 获取当前登录用户的信息。
     *
     * HTTP请求: GET /api/users/me
     *
     * @return 包含当前用户DTO的ResponseEntity，状态码200
     * @throws ResourceNotFoundException 如果无法获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        // 1. 从 SecurityContextHolder 获取 Authentication 对象
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. 检查 Authentication 对象是否存在以及是否已认证
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("User", "current", "Not authenticated");
        }

        // 3. 获取 Principal (通常是 UserDetails 对象或用户名字符串)
        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof UserDetails) {
            // 如果 Principal 是 UserDetails 实例，直接获取用户名
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            // 如果 Principal 是字符串 (有时可能发生)，直接使用它作为用户名
            username = (String) principal;
        } else {
            // 无法识别 Principal 类型
            throw new ResourceNotFoundException("User", "current", "Cannot determine username from principal");
        }

        // 4. 使用用户名从数据库中查找完整的 User 对象
        //    这里我们假设 UserDetails 的 username 就是数据库中的 username
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        // 5. 将 User 实体转换为 UserDto
        UserDto userDto = convertToDto(user);

        // 6. 返回 200 OK 和用户 DTO
        return ResponseEntity.ok(userDto);
    }

    /**
     * 将User实体转换为UserDto
     * 用于在返回给客户端前移除敏感信息（如密码）
     *
     * @param user 用户实体
     * @return 用户DTO，不包含敏感信息
     */
    private UserDto convertToDto(User user) {
        // 创建一个新的UserDto对象
        UserDto userDto = new UserDto();

        // 设置各个字段的值
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setEmail(user.getEmail());
        userDto.setCreatedAt(user.getCreatedAt());
        userDto.setUpdatedAt(user.getUpdatedAt());

        // 注意：不包含密码字段，保证安全

        // 返回填充好的DTO对象
        return userDto;
    }

    // TODO: 实现创建用户的API接口
    // @PostMapping
    // public ResponseEntity<UserDto> createUser(@RequestBody UserRegistrationDto registrationDto) { ... }

    // TODO: 实现更新用户的API接口
    // @PutMapping("/{id}")
    // public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody UserDto userDto) { ... }

    // TODO: 实现删除用户的API接口
    // @DeleteMapping("/{id}")
    // public ResponseEntity<?> deleteUser(@PathVariable Long id) { ... }
}
