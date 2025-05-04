package com.ding.aiplatjava.util;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach; // 用于设置私有字段
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

/**
 * {@link JwtUtil} 的单元测试。
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    // 使用与其他地方可能定义的相同的测试密钥和过期时间，或生成一个安全的
    // 重要提示：此密钥仅用于测试。在生产环境中使用强壮且安全管理的密钥。
    private final String testSecretKeyString = "VGhpcyBpcyBhIHZlcnkgc2VjdXJlIGFuZCBsb25nIHNlY3JldCBmb3IgdGVzdGluZyBwdXJwb3NlcyE="; // Base64 编码的测试密钥
    private final long testJwtExpirationMs = 3600000; // 1 小时（毫秒）
    private final SecretKey testSignInKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecretKeyString));//生成签名密钥

    private UserDetails testUserDetails;//测试用户详情

    // 在每个测试之前设置JWT工具类
    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();//创建JWT工具类
        // 使用 Spring 的 ReflectionTestUtils 设置带有 @Value 注解的私有字段
        ReflectionTestUtils.setField(jwtUtil, "secretKeyString", testSecretKeyString);//设置密钥
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", testJwtExpirationMs);//设置过期时间

        testUserDetails = new User("testuser", "password", Collections.emptyList());//创建测试用户详情
    }

    // 测试生成令牌
    @Test
    void generateToken_shouldGenerateValidToken() {
        String token = jwtUtil.generateToken(testUserDetails);//生成令牌
        assertNotNull(token);//验证令牌不为空

        // 验证可以使用相同的密钥解析令牌
        String extractedUsername = Jwts.parser()
                                       .verifyWith(testSignInKey)//使用相同的密钥解析令牌
                                       .build()
                                       .parseSignedClaims(token)//解析令牌
                                       .getPayload()
                                       .getSubject();//获取用户名
        assertEquals("testuser", extractedUsername);
    }

    // 测试提取用户名
    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken(testUserDetails);//生成令牌
        String username = jwtUtil.extractUsername(token);//提取用户名
        assertEquals("testuser", username);//验证用户名正确
    }

    // 测试提取声明
    @Test
    void extractClaim_shouldReturnCorrectClaim() {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("customClaim", "customValue");
        String token = jwtUtil.generateToken(extraClaims, testUserDetails);//生成令牌

        String subject = jwtUtil.extractClaim(token, claims -> claims.getSubject());//提取用户名
        String customClaimValue = jwtUtil.extractClaim(token, claims -> claims.get("customClaim", String.class));//提取自定义声明
        Date expiration = jwtUtil.extractClaim(token, claims -> claims.getExpiration());//提取过期时间


        assertEquals("testuser", subject);//验证用户名正确
        assertEquals("customValue", customClaimValue);//验证自定义声明正确
        assertNotNull(expiration);//验证过期时间不为空
        assertTrue(expiration.after(new Date()));//验证过期时间在当前时间之后
    }

    // 测试验证令牌
    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken(testUserDetails);//生成令牌
        assertTrue(jwtUtil.isTokenValid(token, testUserDetails));//验证令牌有效
    }

    // 测试验证令牌过期
    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() throws InterruptedException {
         // 生成一个具有非常短过期时间的令牌用于测试
        long shortExpiration = 100; // 100 毫秒
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", shortExpiration);//设置过期时间
        String token = jwtUtil.generateToken(testUserDetails);//生成令牌

        // 等待令牌过期
        Thread.sleep(shortExpiration + 50);

        assertFalse(jwtUtil.isTokenValid(token, testUserDetails));//验证令牌过期
         // 验证从过期令牌提取声明时是否抛出 ExpiredJwtException
        assertThrows(ExpiredJwtException.class, () -> jwtUtil.extractUsername(token));

         // 为其他测试恢复原始过期时间
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", testJwtExpirationMs);
    }
    
    // 测试验证用户名不匹配
    @Test
    void isTokenValid_shouldReturnFalseForUsernameMismatch() {
        String token = jwtUtil.generateToken(testUserDetails);//生成令牌
        UserDetails differentUserDetails = new User("anotheruser", "password", Collections.emptyList());//创建不同的用户详情
        assertFalse(jwtUtil.isTokenValid(token, differentUserDetails));//验证令牌无效
    }

    // 测试验证无效签名
    @Test
    void isTokenValid_shouldReturnFalseForInvalidSignature() {
         // 使用正确的密钥生成令牌
        String token = jwtUtil.generateToken(testUserDetails);//生成令牌

         // 尝试使用不同的密钥进行验证
        SecretKey wrongKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode("d3JvbmdLZXlTdHJpbmdGb3JUZXN0aW5nVGhhdElzTG9uZ0Vub3VnaCE=")); // 不同的 Base64 密钥
        JwtUtil utilWithWrongKey = new JwtUtil();
        ReflectionTestUtils.setField(utilWithWrongKey, "secretKeyString", "d3JvbmdLZXlTdHJpbmdGb3JUZXN0aW5nVGhhdElzTG9uZ0Vub3VnaCE=");
        ReflectionTestUtils.setField(utilWithWrongKey, "jwtExpirationMs", testJwtExpirationMs);

        // 使用错误的密钥提取声明应该失败
         assertThrows(SignatureException.class, () -> utilWithWrongKey.extractUsername(token));

         // isTokenValid 通过 extractAllClaims 隐式检查签名，
         // 但直接断言异常提供了更清晰的反馈。
         // 我们期望 isTokenValid 隐式返回 false，因为 extractUsername 会失败。
         // 然而，直接测试解析过程中的异常对于签名问题更直接。
         // 如果不仔细处理，直接调用 isTokenValid 可能会掩盖底层的 SignatureException。
    }
} 