package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 案例7：接口限流 Controller（INCR + EXPIRE）
 * <p>
 * 测试接口（10次正常，第11次触发限流，窗口60秒后恢复）：
 * GET /api/rate-limit/test  接口限流演示
 */
@RestController
@RequestMapping("/api/rate-limit")
@RequiredArgsConstructor
public class RateLimitController {

    private final RateLimitService rateLimitService;

    /**
     * 限流测试接口：每个 IP 60 秒内最多请求 {@code app.rate-limit.max-requests} 次
     */
    @GetMapping("/test")
    public Result<Map<String, Object>> test(HttpServletRequest request) {
        // 获取客户端 IP（学习案例直接取 remoteAddr）
        String clientIp = getClientIp(request);

        // 执行限流检查：超过阈值抛出 BizException(429)
        rateLimitService.tryAcquire(clientIp);

        return Result.success(Map.of(
                "ip", clientIp,
                "message", "请求成功，未触发限流"
        ));
    }

    /** 获取客户端 IP（生产环境需考虑 X-Forwarded-For 代理头） */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}