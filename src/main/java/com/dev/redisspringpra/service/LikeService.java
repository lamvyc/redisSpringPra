package com.dev.redisspringpra.service;

import com.dev.redisspringpra.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 案例3：点赞统计（Set 数据结构）
 * <p>
 * 为什么点赞用 Set？
 * - 点赞的核心动作是「点赞/取消/判断是否已赞/统计数量」，天然是集合操作；
 * - Set 元素唯一，同一用户重复点赞不会重复计数（SADD 自动去重）；
 * - SISMEMBER O(1) 判断是否已赞、SCARD O(1) 统计数量，性能极高。
 * <p>
 * 如果换其他结构有什么问题？
 * - String 计数（INCR）：无法判断「谁」点过赞，无法取消点赞；
 * - List：元素可重复，无法去重，无法 O(1) 判断是否存在。
 * <p>
 * 为什么用 StringRedisTemplate？
 * - Set 成员是 userId（纯字符串），StringRedisTemplate 存储无引号，可读性好；
 * - RedisTemplate 会把 "1001" 序列化成 "\"1001\""，与 redis-cli 手动 SADD 的数据不一致。
 * <p>
 * 存储结构：like:product:{productId} 是一个 Set，成员是 userId
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private final StringRedisTemplate stringRedisTemplate;

    /** 点赞：SADD like:product:1 1001 */
    public boolean like(Long productId, Long userId) {
        String key = RedisKeyConstants.LIKE_PRODUCT + productId;
        // SADD：返回 1 表示新增成功（之前未点赞），返回 0 表示已存在（重复点赞）
        Long added = stringRedisTemplate.opsForSet().add(key, String.valueOf(userId));
        boolean success = added != null && added > 0;
        log.debug("【点赞】key={}, userId={}, 结果={}", key, userId, success ? "点赞成功" : "已点过赞");
        return success;
    }

    /** 取消点赞：SREM like:product:1 1001 */
    public boolean unlike(Long productId, Long userId) {
        String key = RedisKeyConstants.LIKE_PRODUCT + productId;
        // SREM：返回 1 表示删除成功，返回 0 表示本来就没点过赞
        Long removed = stringRedisTemplate.opsForSet().remove(key, String.valueOf(userId));
        boolean success = removed != null && removed > 0;
        log.debug("【取消点赞】key={}, userId={}, 结果={}", key, userId, success ? "取消成功" : "未点过赞");
        return success;
    }

    /** 判断是否已点赞：SISMEMBER like:product:1 1001 */
    public boolean isLiked(Long productId, Long userId) {
        String key = RedisKeyConstants.LIKE_PRODUCT + productId;
        // SISMEMBER：O(1) 判断成员是否存在
        Boolean exists = stringRedisTemplate.opsForSet().isMember(key, String.valueOf(userId));
        log.debug("【判断点赞】key={}, userId={}, 结果={}", key, userId, exists);
        return Boolean.TRUE.equals(exists);
    }

    /** 点赞统计：SCARD like:product:1 */
    public Long likeCount(Long productId) {
        String key = RedisKeyConstants.LIKE_PRODUCT + productId;
        // SCARD：返回集合元素个数（点赞总数）
        Long count = stringRedisTemplate.opsForSet().size(key);
        log.debug("【点赞统计】key={}, 点赞数={}", key, count);
        return count == null ? 0L : count;
    }

    /**
     * 共同好友/共同点赞用户：SINTER like:product:1 like:product:2
     * <p>
     * Set 亮点：支持集合运算（交集/并集/差集），
     * 如「两个商品的共同点赞用户」一次命令即可算出。
     */
    public Set<String> commonLikers(Long productId1, Long productId2) {
        String key1 = RedisKeyConstants.LIKE_PRODUCT + productId1;
        String key2 = RedisKeyConstants.LIKE_PRODUCT + productId2;
        // SINTER：求两个集合的交集
        Set<String> common = stringRedisTemplate.opsForSet().intersect(key1, key2);
        log.debug("【共同点赞】key1={}, key2={}, 交集={}", key1, key2, common);
        return common;
    }
}