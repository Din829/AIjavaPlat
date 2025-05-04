package com.ding.aiplatjava.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 加密工具类
 * 提供对称加密（AES）功能，用于安全地存储敏感数据，如API Token。
 * 使用 AES/CBC/PKCS5Padding 模式，并将 IV (Initialization Vector) 与密文一起存储。
 * 
 * 注意：密钥管理是关键，当前密钥从 application.properties 读取，
 * **生产环境强烈建议使用更安全的密钥管理机制** (如环境变量、配置中心、Vault等)。
 */
@Component
public class EncryptionUtil {

    private static final Logger log = LoggerFactory.getLogger(EncryptionUtil.class); // 添加日志记录器
    private static final String ALGORITHM = "AES";// 加密算法
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding"; // 使用CBC模式和PKCS5Padding
    private static final int IV_LENGTH_BYTES = 16; // AES IV 通常为 16 字节

    private final SecretKey secretKey;// 对称加密密钥
    private final SecureRandom secureRandom = new SecureRandom();// 随机数生成器

    /**
     * 构造函数，从配置中注入密钥并初始化。
     * @param base64Key Base64编码的密钥字符串 (应为32字节长，对应AES-256)
     */
    public EncryptionUtil(@Value("${app.security.encryption-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);// 解码密钥
        if (keyBytes.length != 32) {
            // 在实际应用中，更好的做法是启动时失败或记录严重错误
            throw new IllegalArgumentException("Invalid AES key length. Expected 32 bytes for AES-256.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);// 初始化对称加密密钥
    }

    /**
     * 加密明文字符串。
     *
     * @param plainText 要加密的明文。
     * @return Base64编码的加密字符串，格式为 IV + Ciphertext。
     * @throws RuntimeException 如果加密过程中发生错误。
     */
    public String encrypt(String plainText) {
        try {
            // 1. 生成随机 IV，意思就是随机生成一个16字节的数组
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

            // 2. 初始化 Cipher，意思就是初始化一个加密器，使用CBC模式和PKCS5Padding
            //CBC：Cipher Block Chaining，密码分组链接模式，是一种对称加密算法，将明文分组加密，然后将密文分组与前一个密文分组进行异或运算，从而形成密文分组。
            //PKCS5Padding：PKCS5填充，是一种对称加密算法，将明文分组加密，然后将密文分组与前一个密文分组进行异或运算，从而形成密文分组。
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

            // 3. 加密，意思就是加密明文，返回加密后的字节数组
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 4. 组合 IV 和密文 (IV 在前)，意思就是将IV和密文组合成一个字节数组
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);
            byte[] combined = byteBuffer.array();

            // 5. Base64 编码，意思就是将字节数组编码成Base64字符串
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            // 在实际应用中，应该进行更细致的异常处理和日志记录
            throw new RuntimeException("Error during encryption", e);
        }
    }

    /**
     * 解密 Base64 编码的加密字符串。
     *
     * @param encryptedText Base64编码的加密字符串 (格式应为 IV + Ciphertext)。
     * @return 解密后的明文字符串。
     * @throws RuntimeException 如果解密过程中发生错误 (例如密钥不匹配、数据损坏等)。
     */
    public String decrypt(String encryptedText) {
        // --- 日志增强: 打印输入值 ---
        log.debug("Decrypting input text: '{}'", encryptedText);
        if (encryptedText == null || encryptedText.isEmpty()) {
            log.error("Input encrypted text is null or empty.");
            throw new IllegalArgumentException("不能解密空的或 null 的文本");
        }
        try {
            // 1. Base64 解码
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            // --- 日志增强: 打印解码后长度 ---
            log.debug("Base64 decoded length: {} bytes", combined.length);

            // 2. 检查解码后的字节数组是否小于IV长度
            if (combined.length < IV_LENGTH_BYTES) {
                throw new GeneralSecurityException("Invalid encrypted text: too short to contain IV.");
            }

            // 3. 分离 IV 和密文
            ByteBuffer byteBuffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byteBuffer.get(iv);
            byte[] encryptedBytes = new byte[byteBuffer.remaining()];//意思就是获取剩余的字节数组
            byteBuffer.get(encryptedBytes);
            // --- 日志增强: 打印分离出的 IV 和密文 (Base64 格式) ---
            log.debug("Separated IV (Base64): {}", Base64.getEncoder().encodeToString(iv));
            log.debug("Separated Ciphertext (Base64): {}", Base64.getEncoder().encodeToString(encryptedBytes));

            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);//意思就是初始化一个IV参数

            // 4. 初始化 Cipher，意思就是初始化一个解密器，使用CBC模式和PKCS5Padding
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

            // 5. 解密,意思就是解密密文，返回解密后的字节数组
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            // 6. 转换为字符串，将字节数组转换为字符串
            String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
            log.debug("Decryption successful.");
            return decryptedString;
        } catch (IllegalArgumentException e) { // 捕获 Base64 解码错误
            log.error("Base64 decoding failed for input text: '{}'. Error: {}", encryptedText, e.getMessage(), e);
            throw new RuntimeException("Base64 解码失败", e);
        } catch (GeneralSecurityException e) {
            // --- 日志增强: 打印详细安全异常 ---
            log.error("Decryption failed due to security exception. Error: {}", e.getMessage(), e);
            // 在实际应用中，应该进行更细致的异常处理和日志记录
            throw new RuntimeException("Error during decryption", e);
        } catch (Exception e) { // 捕获其他意外错误
            log.error("Unexpected error during decryption for input: '{}'. Error: {}", encryptedText, e.getMessage(), e);
            throw new RuntimeException("解密过程中发生意外错误", e);
        }
    }
} 