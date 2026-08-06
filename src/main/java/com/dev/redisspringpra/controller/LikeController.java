package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * 案例3：点赞统计 Controller（Set）
 * <p>
 * 测试接口：
 * POST /api/like/{productId}/{userId}    点赞
 * DELETE /api/like/{productId}/{userId}  取消点赞
 * GET /api/like/{productId}/check?userId=1  判断是否已赞
 * GET /api/like/{productId}/count           点赞统计
 * GET /api/like/{productId}/common?productId2=2  共同点赞用户
 */
@RestController
@RequestMapping("/api/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    /** 点赞 */
    @PostMapping("/{productId}/{userId}")
    public Result<Boolean> like(@PathVariable Long productId, @PathVariable Long userId) {
        return Result.success(likeService.like(productId, userId));
    }

    /** 取消点赞 */
    @DeleteMapping("/{productId}/{userId}")
    public Result<Boolean> unlike(@PathVariable Long productId, @PathVariable Long userId) {
        return Result.success(likeService.unlike(productId, userId));
    }

    /** 判断是否已点赞 */
    @GetMapping("/{productId}/check")
    public Result<Map<String, Object>> isLiked(@PathVariable Long productId, @RequestParam Long userId) {
        return Result.success(Map.of(
                "productId", productId,
                "userId", userId,
                "liked", likeService.isLiked(productId, userId)
        ));
    }

    /** 点赞统计 */
    @GetMapping("/{productId}/count")
    public Result<Map<String, Object>> count(@PathVariable Long productId) {
        return Result.success(Map.of(
                "productId", productId,
                "likeCount", likeService.likeCount(productId)
        ));
    }

    /** 两个商品的共同点赞用户（Set 交集） */
    @GetMapping("/{productId}/common")
    public Result<Map<String, Object>> common(@PathVariable Long productId, @RequestParam Long productId2) {
        Set<String> commonUsers = likeService.commonLikers(productId, productId2);
        return Result.success(Map.of(
                "productId1", productId,
                "productId2", productId2,
                "commonUsers", commonUsers
        ));
    }
}