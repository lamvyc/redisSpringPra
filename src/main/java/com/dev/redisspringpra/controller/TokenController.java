package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 案例6：登录 Token Controller（String + TTL）
 * <p>
 * 测试接口：
 * POST /api/token/login?userId=1       登录（获取 Token）
 * GET  /api/token/verify?token=xxx     校验 Token（自动续期）
 * POST /api/token/logout?token=xxx     退出登录（删除 Token）
 */
@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    /** 登录：返回 Token */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam Long userId) {
        String token = tokenService.login(userId);
        return Result.success(Map.of(
                "userId", userId,
                "token", token,
                "tip", "请求其他接口时携带此 Token"
        ));
    }

    /** 校验 Token（每次请求校验，自动续期 -> 滑动过期） */
    @GetMapping("/verify")
    public Result<User> verify(@RequestParam String token) {
        return Result.success(tokenService.verifyToken(token));
    }

    /** 退出登录：删除 Token，服务端主动失效 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestParam String token) {
        tokenService.logout(token);
        return Result.success();
    }
}