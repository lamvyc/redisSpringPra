package com.dev.redisspringpra.service;

import com.dev.redisspringpra.common.BizException;
import com.dev.redisspringpra.constant.RedisKeyConstants;
import com.dev.redisspringpra.entity.Product;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.repository.MockDb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 案例8：缓存穿透防护（缓存空对象 + 布隆过滤器思想）
 * <p>
 * 什么是缓存穿透？
 * - 恶意请求大量访问「数据库中不存在」的数据（如 id=-1、随机 id）；
 * - 缓存永远不命中 → 请求每次都打到数据库 → 数据库压力巨大甚至宕机。
 * <p>
 * 解决方案（双保险）：
 * 1. 缓存空对象：查不到数据时也缓存空对象（短 TTL 3 分钟），
 *    后续相同查询直接命中缓存，不再打 DB；
 * 2. 布隆过滤器思想：请求先经过「存在性检查」，不存在的 key 直接拒绝，
 *    从源头拦截无效查询。
 * <p>
 * 本案例用 Redis Set 模拟布隆过滤器：
 * - bloom:user 集合存放所有「有效用户Id」；
 * - SISMEMBER O(1) 判断 id 是否可能存在，不存在直接返回，不查 DB 不查缓存。
 * <p>
 * 为什么真实项目用布隆过滤器而不是 Set？
 * - 数据量大时（亿级）Set 太占内存，布隆过滤器用位数组 + 多次哈希，体积小 10 倍以上；
 * - 本案例 Set 仅为演示「先过滤再查询」的思想。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachePenetrationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final MockDb mockDb;

    /** 空对象缓存 TTL：3 分钟（比正常缓存短，防止数据插入后长期读不到） */
    private static final Duration EMPTY_TTL = Duration.ofMinutes(3);

    /** 正常缓存 TTL：30 分钟 */
    private static final Duration NORMAL_TTL = Duration.ofMinutes(30);

    /** 模拟布隆过滤器的 Set key（用户） */
    private static final String BLOOM_USER = "bloom:user";

    /** 空对象标记 */
    private static final String EMPTY_VALUE = "EMPTY";

    /**
     * 查询商品（带缓存穿透防护）
     * <p>
     * 防护流程：
     * 1. 查缓存 → 命中空对象直接返回 null（不再查 DB）；
     * 2. 缓存未命中 → 查 DB；
     * 3. DB 不存在 → 写入空对象缓存（短 TTL）；
     * 4. DB 存在 → 写入正常缓存。
     */
    public Product getProductWithProtection(Long productId) {
        String key = RedisKeyConstants.PRODUCT_DETAIL + productId;

        // 1. 查缓存
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("【缓存命中】key={}, value={}", key, cached);
            // 命中空对象：说明之前查过 DB 不存在，直接返回 null
            if (EMPTY_VALUE.equals(cached)) {
                return null;
            }
            return (Product) cached;
        }
        log.debug("【缓存未命中】key={}，查数据库", key);

        // 2. 查 DB
        Optional<Product> dbResult = mockDb.findProductById(productId);

        if (dbResult.isEmpty()) {
            // 3a. DB 不存在 → 写空对象，TTL 设短，防止数据插入后长期缓存空值
            redisTemplate.opsForValue().set(key, EMPTY_VALUE, EMPTY_TTL);
            log.warn("【缓存穿透防护】商品 {} 不存在，写入空对象缓存，TTL={}分钟", productId, EMPTY_TTL.toMinutes());
            return null;
        }

        // 3b. DB 存在 → 写正常缓存
        Product product = dbResult.get();
        redisTemplate.opsForValue().set(key, product, NORMAL_TTL);
        log.debug("【缓存重建】key={}", key);
        return product;
    }

    /**
     * 布隆过滤器思想演示：查询用户前先经过过滤器
     * <p>
     * 流程：
     * 1. 用户注册时把 userId 加入 bloom:user 集合（本案例模拟预置）；
     * 2. 查询时先 SISMEMBER 判断 userId 是否存在：
     *    - 不存在 → 直接拒绝（不查缓存、不查 DB），大量无效请求在这里被拦截；
     *    - 存在 → 继续走「缓存 → DB」正常流程。
     */
    public User getUserWithBloomFilter(Long userId) {
        // 1. 布隆过滤器思想：判断 userId 是否存在
        Boolean maybeExists = redisTemplate.opsForSet().isMember(BLOOM_USER, String.valueOf(userId));
        log.debug("【过滤器】userId={} 是否存在={}", userId, maybeExists);

        if (!Boolean.TRUE.equals(maybeExists)) {
            log.warn("【过滤器拦截】userId={} 不可能存在，直接拒绝，不查DB", userId);
            throw new BizException("用户不存在（已被过滤器拦截）");
        }

        // 2. 正常缓存查询（复用案例1的缓存 Key）
        String key = RedisKeyConstants.USER_INFO + userId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("【缓存命中】key={}", key);
            return (User) cached;
        }

        // 3. 查 DB
        User user = mockDb.findUserById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        // 4. 写缓存
        redisTemplate.opsForValue().set(key, user, NORMAL_TTL);
        log.debug("【缓存重建】key={}", key);
        return user;
    }

    /**
     * 模拟用户注册：把 userId 加入「布隆过滤器」（本案例为 Set）
     * <p>
     * 真实项目：用户上线时批量把有效 ID 加载进布隆过滤器。
     */
    public void registerUserId(Long userId) {
        redisTemplate.opsForSet().add(BLOOM_USER, String.valueOf(userId));
        log.debug("【注册过滤器】userId={} 已加入 bloom:user", userId);
    }
}