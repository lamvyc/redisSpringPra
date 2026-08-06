package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.entity.Product;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.service.CachePenetrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 案例8：缓存穿透防护 Controller
 * <p>
 * 测试接口：
 * GET  /api/penetration/product/{id}   查询商品（缓存空对象防护）
 * GET  /api/penetration/user/{id}?register=true  查询用户（布隆过滤器思想）
 * POST /api/penetration/user/{id}/register       注册用户ID到过滤器
 */
@RestController
@RequestMapping("/api/penetration")
@RequiredArgsConstructor
public class CachePenetrationController {

    private final CachePenetrationService cachePenetrationService;

    /**
     * 查商品（缓存空对象防护）
     * 测试：GET /api/penetration/product/999 两次
     * 第一次返回 null 并写入空对象缓存，第二次直接在缓存命中空对象，不查 DB
     */
    @GetMapping("/product/{id}")
    public Result<Map<String, Object>> getProduct(@PathVariable Long id) {
        Product product = cachePenetrationService.getProductWithProtection(id);
        return Result.success(Map.of(
                "productId", id,
                "exists", product != null,
                "product", product
        ));
    }

    /**
     * 查用户（布隆过滤器思想）
     * 测试：GET /api/penetration/user/999（未注册 → 被过滤器直接拦截）
     *       GET /api/penetration/user/1（注册过 → 正常查询）
     */
    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        return Result.success(cachePenetrationService.getUserWithBloomFilter(id));
    }

    /** 注册用户ID到过滤器（模拟用户上线） */
    @PostMapping("/user/{id}/register")
    public Result<Void> register(@PathVariable Long id) {
        cachePenetrationService.registerUserId(id);
        return Result.success();
    }
}