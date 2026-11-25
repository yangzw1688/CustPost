package com.kaiyun.infrastructure.storage;

import com.kaiyun.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** 接口令牌使用独立于数据库的部署密钥加密；查询接口只返回是否已配置。 */
@Component
public class ConfigurationSecretCodec {
    private final String configuredKey;
    private final SecureRandom random = new SecureRandom();
    public ConfigurationSecretCodec(@Value("${app.integration.encryption-key:}") String key) { configuredKey = key.trim(); }

    public String encrypt(String value) {
        try {
            byte[] nonce = new byte[12]; random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
        } catch (GeneralSecurityException error) { throw new BusinessException(500, "接口令牌加密失败"); }
    }
    public String decrypt(String value) {
        try {
            byte[] encoded = Base64.getDecoder().decode(value);
            if (encoded.length < 28) throw new GeneralSecurityException("Invalid ciphertext");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, encoded, 0, 12));
            return new String(cipher.doFinal(encoded, 12, encoded.length - 12), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException error) {
            throw new BusinessException(503, "接口令牌无法解密，请核对部署密钥或重新设置令牌");
        }
    }
    private SecretKeySpec key() {
        try {
            byte[] value = Base64.getDecoder().decode(configuredKey);
            if (value.length == 16 || value.length == 32) return new SecretKeySpec(value, "AES");
        } catch (IllegalArgumentException ignored) { }
        throw new BusinessException(400, "请先配置 INTEGRATION_ENCRYPTION_KEY，使用 Base64 编码的16或32字节随机密钥");
    }
}
