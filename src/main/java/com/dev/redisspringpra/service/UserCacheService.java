package com.dev.redisspringpra.service;

import com.dev.redisspringpra.common.BizException;
import com.dev.redisspringpra.constant.RedisKeyConstants;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 案例1：用户信息缓存（String 数据结构）
 * <p>
 * 技术方案：String + Cache Aside（旁路缓存）模式
 * <p>
 * 核心流程：
 * 查询：先查 Redis → 命中直接返回 → 未命中查 DB → 写入 Redis（缓存重建）
 * 更新：先更新 DB → 再删除 Redis 缓存
 * <p>
 * 为什么用 String？
 * - 用户信息是「整体读写」的对象，序列化为 JSON 存 String 即可；
 * - 如果用户信息需要频繁改某个字段（如积分），才适合 Hash。
 * <p>
 * 为什么删除缓存而不是更新缓存？
 * - 更新缓存需要先把 DB 新数据序列化再写，且可能写错（两次更新顺序不一致）；
 * - 删除缓存更简单，下次查询时「缓存重建」自然拿到最新数据（延迟加载思想）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    // 显式构造方法（如需手动注入可参考）
    //    public UserCacheService(RedisTemplate<String, Object> redisTemplate, UserRepository userRepository) {
    //        this.redisTemplate = redisTemplate;
    //        this.userRepository = userRepository;
    //    }

    /** 缓存过期时间：30 分钟 */
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 查询用户：缓存优先
     * <p>
     * 流程：查缓存 → 命中返回 / 未命中查 DB 写缓存
     */
    public User getUserById(Long userId) {
        // 生成缓存 Key
        String key = RedisKeyConstants.USER_INFO + userId;

        // 1. 先查 Redis 缓存
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("【缓存命中】key={}", key);
            return (User) cached;
        }
        log.debug("【缓存未命中】key={}，查询数据库", key);

        // 2. 未命中，查数据库
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));

        // 3. 缓存重建：写回 Redis 并设置过期时间
        // 为什么设置 TTL？防止缓存数据永远不更新，保证最终一致性
        redisTemplate.opsForValue().set(key, user, CACHE_TTL);
        log.debug("【缓存重建】key={}, ttl={}分钟", key, CACHE_TTL.toMinutes());
        return user;
    }

    /**
     * 修改用户信息：先更新 DB，再删除缓存
     * <p>
     * 为什么先更新 DB 再删缓存？
     * - 如果先删缓存再更新 DB，更新 DB 期间会有大量请求打到 DB（缓存击穿风险）；
     * - Cache Aside 标准做法：先 DB 后删缓存。
     */
    public User updateUser(Long userId, String name, Integer age) {
        String key = RedisKeyConstants.USER_INFO + userId;

        // 1. 先更新数据库
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        if (name != null) {
            user.setName(name);
        }
        if (age != null) {
            user.setAge(age);
        }
        userRepository.save(user);
        log.debug("【更新数据库】userId={}", userId);

        // 2. 再删除缓存（下次查询时重建）
        redisTemplate.delete(key);
        log.debug("【删除缓存】key={}", key);
        return user;
    }

    /**
     * 删除用户缓存（管理员操作时调用）
     */
    public void deleteCache(Long userId) {
        String key = RedisKeyConstants.USER_INFO + userId;
        Boolean deleted = redisTemplate.delete(key);
        log.debug("【删除缓存】key={}, 结果={}", key, deleted);
    }

    /**
     * 查看缓存剩余过期时间（演示用）
     * <p>
     * 面试点：TTL 命令在 RedisTemplate 中对应 getExpire，返回剩余秒数。
     */
    public Long getCacheTtl(Long userId) {
        String key = RedisKeyConstants.USER_INFO + userId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        log.debug("【查询TTL】key={}, 剩余={}秒", key, ttl);
        return ttl;
    }
}

/**
 *
 * */