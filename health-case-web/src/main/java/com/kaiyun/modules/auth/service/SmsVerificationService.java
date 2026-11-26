package com.kaiyun.modules.auth.service;

import com.kaiyun.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SmsVerificationService {
    private static final Logger log = LoggerFactory.getLogger(SmsVerificationService.class);
    private static final String KEY_PREFIX = "health-case:sms:{login}:";
    private static final int COOLDOWN_SECONDS = 60;
    private static final int CODE_LIFETIME_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 5;
    private static final int DAY_SECONDS = 86400;
    private static final int PHONE_DAILY_LIMIT = 5;
    private static final int SOURCE_WINDOW_SECONDS = 600;
    private static final int SOURCE_LIMIT = 30;
    private static final String HEX = "0123456789abcdef";
    private static final DefaultRedisScript<Long> RESERVE = script("reserve-sms.lua");
    private static final DefaultRedisScript<Long> ACTIVATE = script("activate-sms.lua");
    private static final DefaultRedisScript<Long> VERIFY = script("verify-sms.lua");

    private final StringRedisTemplate redis;
    private final LoginSmsSender sender;
    private final byte[] secret;
    private final int dailyLimit;
    private final SecureRandom random = new SecureRandom();

    public SmsVerificationService(StringRedisTemplate redis, LoginSmsSender sender,
            @Value("${app.auth.sms.otp-secret:}") String secret,
            @Value("${app.auth.sms.daily-limit:1000}") int dailyLimit) {
        this.redis = redis;
        this.sender = sender;
        this.secret = secret.trim().getBytes(StandardCharsets.UTF_8);
        if (dailyLimit < 1) throw new IllegalArgumentException("短信每日发送上限必须大于 0");
        this.dailyLimit = dailyLimit;
    }

    public int send(String phone, String address) {
        requireSecret();
        sender.checkConfiguration();
        String phoneKey = digest("phone:" + phone);
        String cooldownKey = KEY_PREFIX + "cooldown:" + phoneKey;
        String sendId = UUID.randomUUID().toString();
        long reservation = execute(RESERVE, Arrays.asList(cooldownKey, KEY_PREFIX + "phone:" + phoneKey,
                        KEY_PREFIX + "daily", KEY_PREFIX + "source:" + digest("source:" + address)),
                sendId, String.valueOf(COOLDOWN_SECONDS), String.valueOf(PHONE_DAILY_LIMIT),
                String.valueOf(dailyLimit), String.valueOf(SOURCE_LIMIT), String.valueOf(DAY_SECONDS),
                String.valueOf(SOURCE_WINDOW_SECONDS));
        if (reservation > 0) {
            throw new BusinessException(429, "请在 " + reservation + " 秒后重新获取验证码");
        }
        if (reservation == -1) throw new BusinessException(429, "该手机号已达到短信发送上限，请稍后再试");
        if (reservation != 0) throw new BusinessException(429, "请求验证码过于频繁，请稍后再试");

        // Preserve an existing code if the provider rejects a resend.
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        sender.send(phone, code);
        long activated = execute(ACTIVATE, Arrays.asList(cooldownKey, KEY_PREFIX + "code:" + phoneKey),
                sendId, codeDigest(phoneKey, code), String.valueOf(MAX_ATTEMPTS), String.valueOf(CODE_LIFETIME_SECONDS));
        if (activated != 1) throw new BusinessException(409, "验证码请求已过期，请重新获取");
        return COOLDOWN_SECONDS;
    }

    public void verify(String phone, String code) {
        requireSecret();
        String phoneKey = digest("phone:" + phone);
        long valid = execute(VERIFY, Collections.singletonList(KEY_PREFIX + "code:" + phoneKey),
                codeDigest(phoneKey, code));
        if (valid != 1) throw new BusinessException(400, "验证码错误或已失效，请重新获取");
    }

    private long execute(DefaultRedisScript<Long> script, List<String> keys, String... arguments) {
        try {
            Long result = redis.execute(script, keys, (Object[]) arguments);
            if (result != null) return result;
        } catch (DataAccessException failure) {
            log.warn("SMS Redis operation failed, type={}", failure.getClass().getSimpleName());
        }
        throw new BusinessException(503, "短信验证码服务暂时不可用，请稍后重试");
    }

    private static DefaultRedisScript<Long> script(String filename) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/auth/" + filename));
        script.setResultType(Long.class);
        return script;
    }

    private void requireSecret() {
        if (secret.length < 32) throw new BusinessException(503, "短信服务尚未配置，请联系管理员");
    }

    private String codeDigest(String phoneKey, String code) {
        return digest("code:" + phoneKey + ":" + code);
    }

    private String digest(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) hex.append(HEX.charAt((item & 255) >>> 4)).append(HEX.charAt(item & 15));
            return hex.toString();
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("无法计算验证码摘要", failure);
        }
    }
}
