package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.dto.UserUpdateRequest;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 案例1：用户信息缓存 Controller（String）
 * <p>
 * 测试接口（运行后访问）：
 * GET  /api/user/{id}          查询用户（走缓存）
 * PUT  /api/user/{id}          修改用户（先更新DB后删除缓存）
 * DELETE /api/user/{id}/cache  删除用户缓存
 * GET  /api/user/{id}/ttl      查看缓存剩余时间
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserCacheController {

    private final UserCacheService userCacheService;

    /** 查询用户信息（首次查库写缓存，之后命中缓存） */
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.success(userCacheService.getUserById(id));
    }

    /** 修改用户信息（先更新DB，再删除缓存） */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return Result.success(userCacheService.updateUser(id, request.getName(), request.getAge()));
    }

    /** 删除用户缓存 */
    @DeleteMapping("/{id}/cache")
    public Result<Void> deleteCache(@PathVariable Long id) {
        userCacheService.deleteCache(id);
        return Result.success();
    }

    /** 查看缓存剩余过期时间 */
    @GetMapping("/{id}/ttl")
    public Result<Map<String, Object>> getTtl(@PathVariable Long id) {
        return Result.success(Map.of("userId", id, "ttlSeconds", userCacheService.getCacheTtl(id)));
    }
}