package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.dto.UserUpdateRequest;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.service.SpringCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 第三阶段：Spring Cache 注解演示
 * <p>
 * 测试接口：
 * GET  /api/cache/user/{id}     @Cacheable 查询（带缓存）
 * PUT  /api/cache/user/{id}     @CachePut  更新（同步刷新缓存）
 * DELETE /api/cache/user/{id}   @CacheEvict 删除缓存
 */
@RestController
@RequestMapping("/api/cache/user")
@RequiredArgsConstructor
public class SpringCacheController {

    private final SpringCacheService springCacheService;

    /** @Cacheable：读取缓存，未命中则查库并写缓存 */
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.success(springCacheService.getUserWithCache(id));
    }

    /** @CachePut：更新DB并刷新缓存 */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.success(springCacheService.updateUserWithCache(id, request.getName(), request.getAge()));
    }

    /** @CacheEvict：删除缓存 */
    @DeleteMapping("/{id}")
    public Result<Void> evict(@PathVariable Long id) {
        springCacheService.evictUserCache(id);
        return Result.success();
    }
}