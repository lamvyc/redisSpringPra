package com.dev.redisspringpra.service;

import com.dev.redisspringpra.common.BizException;
import com.dev.redisspringpra.config.AppProperties;
import com.dev.redisspringpra.constant.RedisKeyConstants;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.repository.MockDb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * 案例6：登录 Token（String + TTL）
 * <p>
 * 为什么 Token 用 String？
 * - Token 本身就是「一个随机串对应一个用户ID」的键值映射，String 最自然；
 * - 设置 TTL 实现会话自动过期，无需定时任务清理；
 * - 验证时 GET 一次 O(1)，比查数据库快得多。
 * <p>
 * 为什么不用 MySQL 存 Token？
 * - Token 是高频读写（每次请求都要校验），放 MySQL 会成为热点瓶颈；
 * - Redis 自带过期机制，天然适合会话管理。
 * <p>
 * 存储结构：
 * user:token:{token}  →  userId
 * TTL：30 分钟，每次请求续期（滑动过期：持续活跃永不掉线）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final AppProperties appProperties;
    private final MockDb mockDb;

    /**
     * 登录：生成 Token 存入 Redis
     * <p>
     * 实际项目登录流程：校验用户名密码 → 生成 token → 存 Redis → 返回前端。
     * 学习案例直接按 userId 登录（跳过密码校验）。
     */
    public String login(Long userId) {
        // 校验用户存在
        mockDb.findUserById(userId).orElseThrow(() -> new BizException("用户不存在"));

        // 生成唯一 Token（UUID 带无连字符格式）
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = RedisKeyConstants.USER_TOKEN + token;

        // 存入 Redis 并设置 30 分钟过期
        Duration expire = Duration.ofMinutes(appProperties.getToken().getExpireMinutes());
        stringRedisTemplate.opsForValue().set(key, String.valueOf(userId), expire);
        log.debug("【登录成功】userId={}, token={}, 有效期={}分钟", userId, token, expire.toMinutes());
        return token;
    }

    /**
     * 校验 Token：每次请求时调用
     * <p>
     * 校验成功后「续期」：重置过期时间，实现滑动过期。
     * 让持续活跃的用户保持登录，不活跃用户自动过期。
     */
    public User verifyToken(String token) {
        String key = RedisKeyConstants.USER_TOKEN + token;
        // 1. 读取 token 对应的 userId
        Object userIdObj = stringRedisTemplate.opsForValue().get(key);
        if (userIdObj == null) {
            throw new BizException(401, "Token 无效或已过期，请重新登录");
        }

        // 2. 续期：重置 TTL（滑动过期）
        Duration expire = Duration.ofMinutes(appProperties.getToken().getExpireMinutes());
        stringRedisTemplate.expire(key, expire);
        log.debug("【Token 续期】token={}, 剩余有效期重置为{}分钟", token, expire.toMinutes());

        // 3. 返回用户信息
        Long userId = Long.valueOf(userIdObj.toString());
        return mockDb.findUserById(userId)
                .orElseThrow(() -> new BizException(401, "用户不存在"));
    }

    /**
     * 退出登录：删除 Token
     * <p>
     * 为什么退出要删 Redis？服务端主动失效，防止 Token 在过期前被继续使用。
     */
    public void logout(String token) {
        String key = RedisKeyConstants.USER_TOKEN + token;
        Boolean deleted = stringRedisTemplate.delete(key);
        log.debug("【退出登录】token={}, 删除结果={}", token, deleted);
    }
}