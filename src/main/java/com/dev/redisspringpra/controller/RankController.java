package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.service.RankService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 案例4：排行榜 Controller（ZSet）
 * <p>
 * 测试接口：
 * POST /api/rank/{userId}/score?points=10   增加积分
 * GET  /api/rank/{userId}/rank              查询排名
 * GET  /api/rank/{userId}/score             查询积分
 * GET  /api/rank/top?n=10                   获取 TopN
 * GET  /api/rank/top/withScores?n=10        获取 TopN（含分数）
 */
@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    /** 增加积分（实时更新排行榜） */
    @PostMapping("/{userId}/score")
    public Result<Void> addScore(@PathVariable Long userId, @RequestParam double points) {
        rankService.addScore(userId, points);
        return Result.success();
    }

    /** 查询用户排名 */
    @GetMapping("/{userId}/rank")
    public Result<Map<String, Object>> getRank(@PathVariable Long userId) {
        return Result.success(Map.of(
                "userId", userId,
                "rank", rankService.getRank(userId)
        ));
    }

    /** 查询用户积分 */
    @GetMapping("/{userId}/score")
    public Result<Map<String, Object>> getScore(@PathVariable Long userId) {
        return Result.success(Map.of(
                "userId", userId,
                "score", rankService.getScore(userId)
        ));
    }

    /** 获取 TopN 用户 ID 列表 */
    @GetMapping("/top")
    public Result<Map<String, Object>> getTopN(@RequestParam(defaultValue = "10") int n) {
        return Result.success(Map.of("top", rankService.getTopN(n)));
    }

    /** 获取 TopN（含分数），返回 [{value: userId, score: 积分}] */
    @GetMapping("/top/withScores")
    public Result<Map<String, Object>> getTopNWithScores(@RequestParam(defaultValue = "10") int n) {
        List<ZSetOperations.TypedTuple<String>> topWithScores = rankService.getTopNWithScores(n);
        List<Map<String, Object>> result = topWithScores.stream()
                .map(t -> Map.<String, Object>of(
                        "userId", t.getValue(),
                        "score", t.getScore())
                )
                .toList();
        return Result.success(Map.of("top", result));
    }
}