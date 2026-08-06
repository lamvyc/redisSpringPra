package com.dev.redisspringpra.service;

import com.dev.redisspringpra.common.BizException;
import com.dev.redisspringpra.config.AppProperties;
import com.dev.redisspringpra.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 案例5：验证码（String + TTL）
 * <p>
 * 为什么验证码用 String？
 * - 验证码是一个「短生命周期」的字符串，SETEX 一次搞定「写入+过期」；
 * - 验证码天然适合 TTL：5 分钟过期，过期自动删除，无需手动清理。
 * <p>
 * 如果换其他结构有什么问题？
 * - Hash：存一个字段用 Hash 大材小用，且无法对整个 key 设置精确 TTL 语义；
 * - Set/List：无序或可重复，不适合「一个手机号一个码」的唯一映射。
 * <p>
 * 核心命令：
 * SETEX user:captcha:{phone} 300 123456    写入验证码并设置 5 分钟过期
 * GET  user:captcha:{phone}               读取验证码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate stringRedisTemplate;
    private final AppProperties appProperties;

    /** 安全随机数生成器 */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成验证码并存入 Redis，自动过期
     * <p>
     * SETNX 防刷限流：60 秒内同一手机号只能发一次，
     * 防止短信接口被刷（每次发送都有成本）。
     */
    public String sendCaptcha(String phone) {
        String key = RedisKeyConstants.USER_CAPTCHA + phone;
        // 防刷 key：60 秒内只允许发送一次
        String rateKey = key + ":rate";

        // SETNX user:captcha:{phone}:rate 1：如果 key 已存在返回 false，说明 60 秒内发过
        Boolean firstTime = stringRedisTemplate.opsForValue().setIfAbsent(rateKey, "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(firstTime)) {
            throw new BizException("发送过于频繁，请 60 秒后再试");
        }

        // 生成 6 位数字验证码
        String captcha = generateCode(appProperties.getCaptcha().getLength());

        // SETEX：写入验证码并设置过期时间（5 分钟）
        Duration expire = Duration.ofMinutes(appProperties.getCaptcha().getExpireMinutes());
        stringRedisTemplate.opsForValue().set(key, captcha, expire);
        log.debug("【发送验证码】phone={}, captcha={}, 有效期={}分钟", phone, captcha, expire.toMinutes());

        // 真实项目这里调用短信服务商发送；学习项目直接返回方便测试
        return captcha;
    }

    /**
     * 校验验证码
     * <p>
     * 校验通过后删除验证码（一次性使用），防止验证码被反复使用。
     */
    public boolean verifyCaptcha(String phone, String inputCode) {
        String key = RedisKeyConstants.USER_CAPTCHA + phone;
        // GET 读取验证码
        String stored = stringRedisTemplate.opsForValue().get(key);
        log.debug("【校验验证码】phone={}, 输入={}, 存储={}", phone, inputCode, stored);

        if (stored == null) {
            throw new BizException("验证码已过期，请重新获取");
        }
        // 比对：忽略大小写
        boolean valid = stored.equalsIgnoreCase(inputCode);
        if (valid) {
            // 校验成功，删除验证码（一次性使用）
            stringRedisTemplate.delete(key);
            log.debug("【校验成功】验证码已消费删除");
        }
        return valid;
    }

    /** 生成指定长度的数字验证码 */
    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}