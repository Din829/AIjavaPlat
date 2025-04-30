package com.ding.aiplatjava.util;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link EncryptionUtil} 的单元测试。
 */
class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    // 使用与 application.properties 中相同的测试密钥
    // AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=
    private final String base64TestKey = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="; 

    @BeforeEach
    void setUp() {
        // 直接构造 EncryptionUtil，传入测试密钥
        encryptionUtil = new EncryptionUtil(base64TestKey);
    }

    /**
     * 测试基本的加密和解密流程是否正常工作。
     */
    @Test
    void encryptDecrypt_Success() {
        String originalText = "这是一个需要加密的敏感API Token";

        // 加密
        String encryptedText = encryptionUtil.encrypt(originalText);
        System.out.println("Encrypted: " + encryptedText);

        // 确认加密后的文本不是原始文本，并且不为空
        assertNotNull(encryptedText);
        assertNotEquals(originalText, encryptedText);
        assertTrue(encryptedText.length() > 0);

        // 解密
        String decryptedText = encryptionUtil.decrypt(encryptedText);
        System.out.println("Decrypted: " + decryptedText);

        // 确认解密后的文本与原始文本相同
        assertEquals(originalText, decryptedText);
    }

    /**
     * 测试使用不同的密钥解密是否会失败 (抛出异常)。
     * 注意：预期会抛出 RuntimeException，因为内部捕获了 GeneralSecurityException。
     */
    @Test
    void decryptWithWrongKey_ShouldFail() {
        String originalText = "some data";
        String encryptedText = encryptionUtil.encrypt(originalText);

        // 创建一个使用不同密钥的 EncryptionUtil 实例
        String wrongBase64Key = Base64.getEncoder().encodeToString(new byte[32]); // 生成一个全零的密钥
        EncryptionUtil wrongKeyUtil = new EncryptionUtil(wrongBase64Key);

        // 断言当使用错误的密钥解密时会抛出 RuntimeException
        assertThrows(RuntimeException.class, () -> {
            wrongKeyUtil.decrypt(encryptedText);
        }, "使用错误的密钥解密应该抛出异常");
    }

    /**
     * 测试解密无效的 Base64 字符串是否会失败。
     */
    @Test
    void decryptInvalidBase64_ShouldFail() {
        String invalidBase64 = "不是有效的Base64编码";
        // IllegalArgumentException (由 Base64 解码器抛出) 会被包装在 RuntimeException 中
        assertThrows(RuntimeException.class, () -> {
            encryptionUtil.decrypt(invalidBase64);
        });
    }

     /**
     * 测试解密格式不正确（缺少 IV）的加密文本是否会失败。
     */
    @Test
    void decryptTruncatedText_ShouldFail() {
        String originalText = "test";
        String encryptedText = encryptionUtil.encrypt(originalText);
        // 故意截断，使其长度小于 IV 长度 (16)
        String truncatedText = encryptedText.substring(0, 10); 

        // GeneralSecurityException 会被包装在 RuntimeException 中
        assertThrows(RuntimeException.class, () -> {
             encryptionUtil.decrypt(truncatedText);
        });
    }

    // 可以添加更多测试，例如加密空字符串、特殊字符等
    @Test
    void encryptEmptyString() {
        String originalText = "";
        String encryptedText = encryptionUtil.encrypt(originalText);
        assertNotNull(encryptedText);
        String decryptedText = encryptionUtil.decrypt(encryptedText);
        assertEquals(originalText, decryptedText);
    }
} 