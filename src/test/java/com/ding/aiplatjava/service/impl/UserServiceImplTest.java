package com.ding.aiplatjava.service.impl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ding.aiplatjava.entity.User;
import com.ding.aiplatjava.mapper.UserMapper;
import com.ding.aiplatjava.service.UserService;

/**
 * {@link UserServiceImpl} 的单元测试。
 */
@ExtendWith(MockitoExtension.class) // 使用 Mockito 扩展
class UserServiceImplTest {

    @Mock // 创建 UserMapper 的模拟对象
    private UserMapper userMapper;

    @Mock // 添加 PasswordEncoder 的模拟对象
    private PasswordEncoder passwordEncoder;

    @InjectMocks // 将模拟对象注入到被测试的 Service 实例中
    private UserServiceImpl userService;

    private User user1; // 通用测试用户数据
    private final Long userId1 = 1L;
    private final String username1 = "testuser";
    private final String email1 = "test@example.com";
    private final String rawPassword = "password"; // 添加原始密码
    private final String encodedPassword = "encodedPassword"; // 添加编码后的密码

    /**
     * 在每个测试方法执行前运行，初始化测试数据和 Mock 行为。
     */
    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        user1 = User.builder()
                .id(userId1)
                .username(username1)
                .email(email1)
                .password(encodedPassword) // 使用编码后的密码
                .createdAt(now.minusDays(1))
                .updatedAt(now.minusDays(1))
                .build();

        // 模拟密码编码器的行为
        // 不再需要全局设置: when(passwordEncoder.encode(anyString())).thenReturn(encodedPassword);
    }

    // --- 测试 findById --- 

    /**
     * 测试根据 ID 成功找到用户的情况。
     */
    @Test
    void findById_Success() {
        // Arrange (准备): 模拟当调用 mapper 的 selectById 时返回 user1
        when(userMapper.selectById(userId1)).thenReturn(user1);

        // Act (执行): 调用 service 的 findById 方法
        Optional<User> foundUser = userService.findById(userId1);

        // Assert (断言): 验证返回的 Optional 包含用户，用户数据正确，并且 mapper 的 selectById 被调用了一次
        assertTrue(foundUser.isPresent(), "用户应该被找到");
        assertEquals(user1, foundUser.get(), "找到的用户应该与预期一致");
        verify(userMapper, times(1)).selectById(userId1); // 验证 mock 方法调用次数
    }

    /**
     * 测试根据 ID 未找到用户的情况。
     */
    @Test
    void findById_NotFound() {
        // Arrange: 模拟当调用 mapper 的 selectById 时返回 null
        when(userMapper.selectById(userId1)).thenReturn(null);

        // Act: 调用 service 的 findById 方法
        Optional<User> foundUser = userService.findById(userId1);

        // Assert: 验证返回的 Optional 为空
        assertFalse(foundUser.isPresent(), "用户不应该被找到");
        verify(userMapper, times(1)).selectById(userId1);
    }

    // --- 测试 findByUsername --- 

    /**
     * 测试根据用户名成功找到用户的情况。
     */
    @Test
    void findByUsername_Success() {
        // Arrange: 模拟 mapper 返回 user1
        when(userMapper.selectByUsername(username1)).thenReturn(user1);

        // Act: 调用 service 方法
        Optional<User> foundUser = userService.findByUsername(username1);

        // Assert: 验证结果存在且正确
        assertTrue(foundUser.isPresent());
        assertEquals(user1, foundUser.get());
        verify(userMapper, times(1)).selectByUsername(username1);
    }

    /**
     * 测试根据用户名未找到用户的情况。
     */
    @Test
    void findByUsername_NotFound() {
        // Arrange: 模拟 mapper 返回 null
        when(userMapper.selectByUsername(username1)).thenReturn(null);

        // Act: 调用 service 方法
        Optional<User> foundUser = userService.findByUsername(username1);

        // Assert: 验证结果为空
        assertFalse(foundUser.isPresent());
        verify(userMapper, times(1)).selectByUsername(username1);
    }

    // --- 测试 findByEmail --- 

    /**
     * 测试根据邮箱成功找到用户的情况。
     */
    @Test
    void findByEmail_Success() {
        // Arrange
        when(userMapper.selectByEmail(email1)).thenReturn(user1);
        // Act
        Optional<User> found = userService.findByEmail(email1);
        // Assert
        assertTrue(found.isPresent());
        assertEquals(user1, found.get());
        verify(userMapper).selectByEmail(email1);
    }

    /**
     * 测试根据邮箱未找到用户的情况。
     */
    @Test
    void findByEmail_NotFound() {
        // Arrange
        when(userMapper.selectByEmail(email1)).thenReturn(null);
        // Act
        Optional<User> found = userService.findByEmail(email1);
        // Assert
        assertFalse(found.isPresent());
        verify(userMapper).selectByEmail(email1);
    }

    // --- 测试 findAll --- 

    /**
     * 测试成功获取所有用户列表的情况。
     */
    @Test
    void findAll_Success() {
        // Arrange: 模拟 mapper 返回包含一个用户的列表
        List<User> users = Collections.singletonList(user1);
        when(userMapper.selectList()).thenReturn(users);

        // Act: 调用 service 方法
        List<User> foundUsers = userService.findAll();

        // Assert: 验证列表不为 null，不为空，并且包含预期的用户
        assertNotNull(foundUsers);
        assertFalse(foundUsers.isEmpty());
        assertEquals(1, foundUsers.size());
        assertEquals(user1, foundUsers.get(0));
        verify(userMapper, times(1)).selectList();
    }

    /**
     * 测试获取用户列表为空的情况。
     */
    @Test
    void findAll_Empty() {
        // Arrange: 模拟 mapper 返回空列表
        when(userMapper.selectList()).thenReturn(Collections.emptyList());

        // Act: 调用 service 方法
        List<User> foundUsers = userService.findAll();

        // Assert: 验证列表不为 null 且为空
        assertNotNull(foundUsers);
        assertTrue(foundUsers.isEmpty());
        verify(userMapper, times(1)).selectList();
    }

    // --- 测试 create --- 

    /**
     * 测试成功创建用户的情况。
     */
    @Test
    void create_Success() {
        // Arrange: 创建一个不带 ID 和时间戳的新用户对象
        User newUser = User.builder()
                            .username("newuser")
                            .email("new@test.com")
                            .password(rawPassword) // 使用原始密码
                            .build();

        // 模拟密码编码行为 (在此测试中需要)
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);

        // Act: 调用 service 的 create 方法
        User createdUser = userService.create(newUser);

        // Assert: 验证返回的用户对象不为 null，其创建和更新时间戳已被设置，
        // 并且验证 mapper 的 insert 方法被正确调用（传入的用户对象时间戳已设置）
        assertNotNull(createdUser);
        assertEquals(encodedPassword, createdUser.getPassword(), "密码应该被编码");
        assertNotNull(createdUser.getCreatedAt(), "创建时间应被设置");
        assertNotNull(createdUser.getUpdatedAt(), "更新时间应被设置");
        assertEquals(createdUser.getCreatedAt(), createdUser.getUpdatedAt(), "创建和更新时间应相同");

        // 验证 mapper 的 insert 方法被调用，并且密码是编码后的
        verify(userMapper, times(1)).insert(argThat(user -> 
            user.getPassword().equals(encodedPassword) &&
            user.getCreatedAt() != null && 
            user.getUpdatedAt() != null
        ));
    }

    // --- 测试 update --- 

    /**
     * 测试成功更新用户的情况。
     */
    @Test
    void update_Success() {
        // Arrange: 准备一个包含 ID 和更新信息的用户对象
        User userToUpdate = User.builder()
                                .id(userId1)
                                .username("updateduser")
                                .email("update@example.com") // 更新邮箱
                                // 不应在 update 中传递密码
                                .build();
        LocalDateTime originalUpdateTs = user1.getUpdatedAt(); // 保存原始更新时间以便比较

        // *** 关键修复：模拟 selectById 在 update 后返回更新后的用户信息 ***
        // 创建一个预期的更新后用户对象，其更新时间应被修改
        User expectedUpdatedUser = User.builder()
                                        .id(userId1)
                                        .username(userToUpdate.getUsername())
                                        .email(userToUpdate.getEmail())
                                        .password(encodedPassword) // 密码应保持不变
                                        .createdAt(user1.getCreatedAt()) // 创建时间不变
                                        .updatedAt(LocalDateTime.now().plusSeconds(1)) // 模拟一个更新后的时间
                                        .build();
        when(userMapper.selectById(userId1)).thenReturn(expectedUpdatedUser);

        // Act: 调用 service 的 update 方法
        User actualUpdatedUser = userService.update(userToUpdate);

        // Assert: 验证返回的用户对象不为 null，其更新时间戳已被更新（与原始不同），
        // 并且验证 mapper 的 updateById 方法被正确调用（传入的用户 ID 正确且更新时间已设置）
        assertNotNull(actualUpdatedUser, "更新后的用户不应为 null");
        assertEquals(expectedUpdatedUser.getUsername(), actualUpdatedUser.getUsername());
        assertEquals(expectedUpdatedUser.getEmail(), actualUpdatedUser.getEmail());
        assertNotNull(actualUpdatedUser.getUpdatedAt(), "更新时间应被设置");
        assertNotEquals(originalUpdateTs, actualUpdatedUser.getUpdatedAt(), "更新时间应该与原始时间不同");

        // 验证 updateById 被调用，传入的对象 ID 正确，并且密码字段为 null (因为 service 实现会清空它)
        verify(userMapper, times(1)).updateById(argThat(user -> 
            user.getId().equals(userId1) && 
            user.getPassword() == null && // 验证 Service 移除了密码
            user.getUpdatedAt() != null // 验证 Service 设置了更新时间
        ));
        // 验证 selectById 在 updateById 之后被调用了一次
        verify(userMapper, times(1)).selectById(userId1);
    }

    // --- 测试 delete --- 

    /**
     * 测试成功删除用户的情况。
     */
    @Test
    void delete_Success() {
        // Arrange: 对于 void 返回类型的方法，如果只验证调用，则无需模拟

        // Act: 调用 service 的 delete 方法
        userService.delete(userId1);

        // Assert: 验证 mapper 的 deleteById 方法被调用了一次，并且传入了正确的 ID
        verify(userMapper, times(1)).deleteById(userId1);
    }

    // --- 测试 registerUser --- (假设存在此测试方法)

    /**
     * 测试成功注册用户的情况。
     */
    @Test
    void registerUser_Success() {
        // Arrange: 创建注册用的用户对象
        User registerRequestUser = User.builder()
                                        .username("registeruser")
                                        .email("register@test.com")
                                        .password(rawPassword) // 使用原始密码
                                        .build();

        // 模拟密码编码行为 (在此测试中需要)
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        
        // 模拟insert后返回带ID的对象 (或者验证insert的参数)
        // 例如: Mockito.doAnswer(invocation -> { ... set ID ... return null; }).when(userMapper).insert(any(User.class));

        // Act: 调用 service 的 registerUser 方法
        User registeredUser = userService.registerUser(registerRequestUser);

        // Assert: 验证返回的用户对象和数据库交互
        assertNotNull(registeredUser);
        assertEquals(encodedPassword, registeredUser.getPassword(), "密码应该被编码");
        assertNotNull(registeredUser.getCreatedAt());
        assertNotNull(registeredUser.getUpdatedAt());

        // 验证 mapper 的 insert 方法被调用，并且密码是编码后的
        verify(userMapper, times(1)).insert(argThat(user -> 
            user.getUsername().equals("registeruser") &&
            user.getPassword().equals(encodedPassword) &&
            user.getCreatedAt() != null && 
            user.getUpdatedAt() != null
        ));
        
        // 验证 passwordEncoder 被调用一次
        verify(passwordEncoder, times(1)).encode(rawPassword);
    }
} 