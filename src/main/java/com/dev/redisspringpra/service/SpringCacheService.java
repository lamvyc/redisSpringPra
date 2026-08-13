package com.dev.redisspringpra.service;

import com.dev.redisspringpra.common.BizException;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 第三阶段：Spring Cache 注解案例
 * <p>
 * 对比案例1手写 RedisTemplate，Spring Cache 用注解简化缓存操作：
 * - @Cacheable  查询：先查缓存，命中直接返回，未命中执行方法并自动写缓存
 * - @CachePut   更新：执行方法后，把返回值写回缓存
 * - @CacheEvict 删除：执行方法后，删除缓存
 * <p>
 * 注解 vs 手写 RedisTemplate：
 * - 注解：开发快、适合「缓存逻辑简单且统一」的场景（如详情页）；
 * - 手写：灵活可控、适合「缓存逻辑复杂」的场景（如分布式锁+缓存重建、双删）。
 * 面试点：Spring Cache 本质还是封装了 RedisTemplate，通过 CacheManager 管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpringCacheService {

    private final UserRepository userRepository;

    /**
     * 查询用户，自动缓存
     * <p>
     * @Cacheable 执行流程：
     * 1. 先查缓存（key = user::1）；
     * 2. 命中 → 直接返回缓存值，不执行方法体；
     * 3. 未命中 → 执行方法查库，返回值自动写入缓存。
     * <p>
     * 为什么不用手写 get/set？注解把样板代码收敛了，普通查询场景更简洁。
     */
    @Cacheable(cacheNames = "user", key = "#userId")
    public User getUserWithCache(Long userId) {
        log.debug("【Spring Cache】缓存未命中，查询数据库 userId={}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
    }

    /**
     * 更新用户信息，并更新缓存
     * <p>
     * @CachePut 执行流程：先执行方法（更新DB），再用返回值更新缓存。
     * 适合「写操作后缓存需要同步刷新」的场景。
     */
    @CachePut(cacheNames = "user", key = "#userId")
    public User updateUserWithCache(Long userId, String name, Integer age) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException("用户不存在"));
        if (name != null) {
            user.setName(name);
        }
        if (age != null) {
            user.setAge(age);
        }
        userRepository.save(user);
        log.debug("【Spring Cache】@CachePut 更新DB并刷新缓存 userId={}", userId);
        return user;
    }

    /**
     * 删除用户缓存
     * <p>
     * @CacheEvict 执行流程：执行方法后删除指定缓存。
     * allEntries = true 表示清空整个 user 缓存区。
     */
    @CacheEvict(cacheNames = "user", key = "#userId")
    public void evictUserCache(Long userId) {
        log.debug("【Spring Cache】@CacheEvict 删除用户缓存 userId={}", userId);
    }
}