package com.dev.redisspringpra.service;

import com.dev.redisspringpra.common.BizException;
import com.dev.redisspringpra.config.AppProperties;
import com.dev.redisspringpra.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 案例7：接口限流（INCR + EXPIRE）
 * <p>
 * 为什么用 INCR + EXPIRE 做限流？
 * - 固定窗口算法：统计「窗口期内请求次数」，超过阈值则拒绝；
 * - INCR 是原子自增（计数器），EXPIRE 设置窗口过期时间，两条命令组合即可实现。
 * <p>
 * 为什么不直接用 MySQL 计数？
 * - 限流是「高频 + 低价值」的计数操作，打数据库会造成巨大压力；
 * - Redis 单线程原子自增，性能极高（10万+ QPS）。
 * <p>
 * 核心命令：
 * INCR rate:limit:192.168.1.1         自增计数（第一次为 1）
 * EXPIRE rate:limit:192.168.1.1 60     设置窗口 60 秒
 * 计数超过阈值 → 拒绝请求
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;
    private final AppProperties appProperties;

    /**
     * 固定窗口限流检查
     * <p>
     * 流程：
     * 1. INCR 自增：第一次请求返回 1；
     * 2. 如果是第一次（count==1），设置窗口过期时间 EXPIRE；
     * 3. 如果计数 > 阈值，拒绝请求。
     * <p>
     * 注意：为什么只第一次设置 EXPIRE？
     * - 如果每次都 EXPIRE，相当于「窗口滑动」，限流会失效；
     * - 只在第一次设置，窗口固定 60 秒。
     */
    public boolean tryAcquire(String clientIp) {
        String key = RedisKeyConstants.RATE_LIMIT + clientIp;
        int maxRequests = appProperties.getRateLimit().getMaxRequests();
        int windowSeconds = appProperties.getRateLimit().getWindowSeconds();

        // INCR：原子自增，返回当前窗口内的请求次数
        Long count = stringRedisTemplate.opsForValue().increment(key);
        log.debug("【限流】key={}, 当前第{}次请求", key, count);

        // 第一次请求设置窗口过期时间
        if (count != null && count == 1) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            log.debug("【限流】设置窗口={}秒", windowSeconds);
        }

        // 超过阈值 → 限流
        if (count != null && count > maxRequests) {
            log.warn("【限流】key={} 触发限流，{}秒内最多{}次", key, windowSeconds, maxRequests);
            throw new BizException(429, "请求过于频繁，请稍后重试");
        }
        return true;
    }
}