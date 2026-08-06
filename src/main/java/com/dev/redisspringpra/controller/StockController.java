package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.BizException;
import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 案例9：分布式锁（Redisson）秒杀库存 Controller
 * <p>
 * 测试接口：
 * POST /api/stock/{productId}/init?stock=10   初始化库存
 * POST /api/stock/{productId}/seckill?userId=1  秒杀（并发测试：100个请求抢10个库存）
 * GET  /api/stock/{productId}                 查询剩余库存
 * <p>
 * 并发测试：用 IDEA 的 HTTP 客户端或 curl 并发发送 100 次秒杀请求，
 * 观察控制台日志：10 次成功 + 90 次库存不足/锁超时。
 */
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    /** 初始化库存 */
    @PostMapping("/{productId}/init")
    public Result<Void> initStock(@PathVariable Long productId, @RequestParam int stock) {
        stockService.initStock(productId, stock);
        return Result.success();
    }

    /** 秒杀扣减库存 */
    @PostMapping("/{productId}/seckill")
    public Result<Map<String, Object>> seckill(@PathVariable Long productId, @RequestParam Long userId) {
        boolean success = stockService.seckill(productId, userId);
        if (!success) {
            throw new BizException("秒杀失败，库存不足或系统繁忙");
        }
        return Result.success(Map.of(
                "userId", userId,
                "productId", productId,
                "message", "抢购成功"
        ));
    }

    /** 查询剩余库存 */
    @GetMapping("/{productId}")
    public Result<Map<String, Object>> getStock(@PathVariable Long productId) {
        return Result.success(Map.of(
                "productId", productId,
                "stock", stockService.getStock(productId)
        ));
    }
}