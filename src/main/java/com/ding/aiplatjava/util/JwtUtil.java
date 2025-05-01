package com.ding.aiplatjava.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT (JSON Web Token) 工具类。
 * 提供生成、解析和验证 JWT 的功能。
 */
@Component
public class JwtUtil {

    // 从配置文件注入 JWT 签名密钥 (Base64编码)
    // !! 生产环境应使用更安全的方式管理密钥 !!
    @Value("${app.security.jwt.secret-key}")
    private String secretKeyString;

    // 从配置文件注入 JWT 过期时间 (毫秒)
    @Value("${app.security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * 从 Token 中提取用户名 (Subject)。
     *
     * @param token JWT 字符串。
     * @return 用户名。
     */
    public String extractUsername(String token) {
        // 调用 extractClaim 方法，传入获取 Subject 的函数
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从 Token 中提取指定的 Claim (声明)。
     * 使用 Function 函数式接口来指定如何从 Claims 中提取所需的值。
     *
     * @param <T>            Claim 的类型。
     * @param token          JWT 字符串。
     * @param claimsResolver 用于从 Claims 中提取特定值的函数。
     * @return 提取出的 Claim 值。
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // 1. 解析 Token 获取所有的 Claims
        final Claims claims = extractAllClaims(token);
        // 2. 应用传入的函数来提取所需的 Claim
        return claimsResolver.apply(claims);
    }

    /**
     * 为给定的 UserDetails 生成 JWT。
     * 使用空 Map 作为额外 Claims。
     *
     * @param userDetails 包含用户信息的 UserDetails 对象。
     * @return 生成的 JWT 字符串。
     */
    public String generateToken(UserDetails userDetails) {
        // 调用重载方法，传入空的 claims Map
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * 为给定的 UserDetails 和额外的 Claims 生成 JWT。
     *
     * @param extraClaims 要添加到 JWT Payload 中的额外声明。
     * @param userDetails 包含用户信息的 UserDetails 对象 (其 getUsername() 将作为 Subject)。
     * @return 生成的 JWT 字符串。
     */
    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        // 使用 Jwts.builder() 构建 JWT
        return Jwts.builder()
                .claims(extraClaims) // 设置额外的 Claims
                .subject(userDetails.getUsername()) // 设置 Subject 为用户名
                .issuedAt(new Date(System.currentTimeMillis())) // 设置签发时间为当前时间
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // 设置过期时间
                .signWith(getSignInKey(), Jwts.SIG.HS256) // 使用 HS256 算法和密钥签名
                // .signWith(getSignInKey(), Jwts.SIG.HS512) // 或者使用更强的 HS512
                .compact(); // 构建并压缩成字符串
    }

    /**
     * 验证 Token 是否有效。
     * 检查 Token 中的用户名是否与 UserDetails 匹配，以及 Token 是否已过期。
     *
     * @param token       JWT 字符串。
     * @param userDetails 用于比较用户名的 UserDetails 对象。
     * @return 如果 Token 有效则返回 true，否则返回 false。
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        // 1. 从 Token 中提取用户名
        final String username = extractUsername(token);
        // 2. 检查提取的用户名是否与 UserDetails 中的用户名匹配，并且 Token 未过期
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * 检查 Token 是否已过期。
     *
     * @param token JWT 字符串。
     * @return 如果 Token 已过期则返回 true，否则返回 false。
     */
    private boolean isTokenExpired(String token) {
        // 提取过期时间 Claim，并与当前时间比较
        return extractExpiration(token).before(new Date());
    }

    /**
     * 从 Token 中提取过期时间 (Expiration) Claim。
     *
     * @param token JWT 字符串。
     * @return 过期时间 Date 对象。
     */
    private Date extractExpiration(String token) {
        // 调用 extractClaim 方法，传入获取 Expiration 的函数
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 解析 JWT 并提取所有的 Claims (声明)。
     * 使用配置的签名密钥来验证 Token 签名。
     *
     * @param token JWT 字符串。
     * @return 包含所有 Claims 的 Claims 对象。
     * @throws io.jsonwebtoken.JwtException 如果 Token 无效 (签名错误、过期、格式错误等)。
     */
    private Claims extractAllClaims(String token) {
        // 使用 Jwts.parser() 构建解析器
        return Jwts.parser()
                .verifyWith(getSignInKey()) // 指定用于验证签名的密钥
                .build()
                .parseSignedClaims(token) // 解析 JWS (签名的 JWT)
                .getPayload(); // 获取 Payload 部分 (Claims)
    }

    /**
     * 获取用于签名和验证的 SecretKey。
     * 将配置文件中 Base64 编码的密钥字符串解码为 SecretKey 对象。
     *
     * @return SecretKey 对象。
     */
    private SecretKey getSignInKey() {
        // 1. Base64 解码密钥字符串
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
        // 2. 使用解码后的字节创建 HMAC-SHA 密钥
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 