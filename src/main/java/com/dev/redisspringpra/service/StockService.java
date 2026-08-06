package com.dev.redisspringpra.service;

import com.dev.redisspringpra.common.BizException;
import com.dev.redisspringpra.constant.RedisKeyConstants;
import com.dev.redisspringpra.repository.MockDb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 案例9：分布式锁（Redisson）—— 秒杀库存扣减
 * <p>
 * 场景问题：
 * - 秒杀场景下，多个线程/多个服务实例同时扣减同一商品库存；
 * - 不用锁会超卖（stock=0 仍被扣成 -1）；
 * - 单机 synchronized 锁只对单实例有效，分布式环境必须用分布式锁。
 * <p>
 * 为什么用 Redisson 而不是手写 SETNX？
 * 手写分布式锁要处理 3 个经典问题：
 * 1. 锁过期时间设多长？—— 业务没跑完锁却过期了（Redisson 看门狗自动续期解决）；
 * 2. 释放锁必须验证「持有者」—— 防止 A 的锁被 B 释放（Redisson 内部 Lua 原子校验）；
 * 3. 获取锁失败重试 —— Redisson tryLock 支持等待时间。
 * <p>
 * Redisson 核心机制：
 * - getLock() 基于 Lua 脚本实现「加锁 + 过期 + 可重入」原子性；
 * - 看门狗（watchdog）默认每 10 秒检查一次，锁未释放则自动续期到 30 秒；
 * - unlock() 通过 Lua 校验「锁持有者 == 当前线程」后才删除 key。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final MockDb mockDb;

    /**
     * 秒杀扣减库存（Redisson 分布式锁版本）
     * <p>
     * 流程：
     * 1. 先用 Redis String 预扣库存（INCR/DECR 原子操作，性能高）；
     * 2. 获取 Redisson 分布式锁保护「数据库库存扣减」临界区；
     * 3. 业务执行完成释放锁。
     * <p>
     * 为什么库存扣减还要锁？
     * - Redis DECR 能保证「Redis 库存」原子，但最终要落库（MockDb），
     *   数据库扣减 + 记录订单等操作必须串行化，否则并发下数据库超卖。
     */
    public boolean seckill(Long productId, Long userId) {
        String stockKey = RedisKeyConstants.STOCK_PRODUCT + productId;
        String lockKey = RedisKeyConstants.LOCK_STOCK_DEDUCT + productId;

        // ===== 阶段1：Redis 预扣库存（不加锁，DECR 天然原子） =====
        Long remain = stringRedisTemplate.opsForValue().decrement(stockKey);
        if (remain == null || remain < 0) {
            // 库存不足：回补库存（把刚才的 -1 加回来）
            stringRedisTemplate.opsForValue().increment(stockKey);
            log.warn("【秒杀失败】productId={} 库存不足", productId);
            return false;
        }
        log.debug("【预扣库存】productId={}, 剩余={}", productId, remain);

        // ===== 阶段2：获取分布式锁，保护数据库操作 =====
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // tryLock：最多等待 3 秒获取锁，锁自动过期 30 秒（看门狗会续期）
            locked = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!locked) {
                // 获取锁失败：回补 Redis 预扣库存
                stringRedisTemplate.opsForValue().increment(stockKey);
                log.warn("【秒杀失败】获取分布式锁超时，productId={}", productId);
                throw new BizException(429, "系统繁忙，请稍后重试");
            }
            log.debug("【获取锁成功】lockKey={}, 线程={}", lockKey, Thread.currentThread().getName());

            // ===== 阶段3：数据库库存扣减（临界区） =====
            // 模拟真实数据库扣减 + 订单创建等耗时业务
            boolean dbSuccess = mockDb.deductStock(productId);
            if (!dbSuccess) {
                stringRedisTemplate.opsForValue().increment(stockKey);
                log.warn("【秒杀失败】DB扣减失败，productId={}", productId);
                return false;
            }
            log.info("【秒杀成功】userId={} 抢到 productId={}", userId, productId);
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stringRedisTemplate.opsForValue().increment(stockKey);
            log.error("【秒杀异常】获取锁被中断", e);
            return false;
        } finally {
            // ===== 阶段4：释放锁 =====
            // 为什么要「判断 isHeldByCurrentThread」？
            // Redisson 的 unlock() 内部已做持有者校验，这里再判断是为了幂等安全
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("【释放锁】lockKey={}", lockKey);
            }
        }
    }

    /**
     * 初始化库存（学习演示用）
     * <p>
     * 为什么库存放 Redis？
     * - 秒杀场景高并发，库存查询/扣减请求量极大，放 MySQL 会打爆数据库；
     * - Redis DECR 原子扣减可支撑 10 万+ QPS，秒杀后再异步落库。
     */
    public void initStock(Long productId, int stock) {
        stringRedisTemplate.opsForValue().set(RedisKeyConstants.STOCK_PRODUCT + productId, String.valueOf(stock));
        log.debug("【初始化库存】productId={}, stock={}", productId, stock);
    }

    /** 查询剩余库存 */
    public Long getStock(Long productId) {
        Object stock = stringRedisTemplate.opsForValue().get(RedisKeyConstants.STOCK_PRODUCT + productId);
        log.debug("【查询库存】productId={}, stock={}", productId, stock);
        return stock == null ? 0L : Long.parseLong(stock.toString());
    }
}