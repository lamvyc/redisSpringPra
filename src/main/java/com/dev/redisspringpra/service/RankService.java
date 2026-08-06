package com.dev.redisspringpra.service;

import com.dev.redisspringpra.constant.RedisKeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 案例4：排行榜（ZSet 数据结构）
 * <p>
 * 为什么排行榜用 ZSet？
 * - ZSet = Set（元素唯一）+ Score（分数），支持按分数排序；
 * - ZADD 追加分数、ZINCRBY 实时加分、ZREVRANGE 取 TopN，天然适合排行榜；
 * - 分数是 double，会自动按分数从低到高排序，取反序得到从高到低。
 * <p>
 * 如果换其他结构有什么问题？
 * - List：需要自己排序、自己维护分数，插入删除成本高；
 * - Set：无分数概念，无法排序；
 * - Hash：存分数需要额外排序逻辑。
 * <p>
 * 存储结构：rank:score 是一个 ZSet：
 * member=userId, score=积分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 增加积分：ZINCRBY rank:score 10 user:1
     * <p>
     * 为什么用 ZINCRBY 而不是 ZADD？
     * - ZADD 会覆盖原有分数；ZINCRBY 是原子累加，适合积分增长场景。
     */
    public void addScore(Long userId, double score) {
        // ZINCRBY key increment member
        Double newScore = stringRedisTemplate.opsForZSet().incrementScore(RedisKeyConstants.RANK_SCORE, String.valueOf(userId), score);
        log.debug("【增加积分】userId={}, +{}分, 当前={}分", userId, score, newScore);
    }

    /**
     * 查询用户排名：ZREVRANK rank:score user:1
     * <p>
     * 注意：ZREVRANK 返回从 0 开始的下标，展示时 +1 就是第几名。
     */
    public Long getRank(Long userId) {
        // 反序排名：分数最高的排第 0 位
        Long rank = stringRedisTemplate.opsForZSet().reverseRank(RedisKeyConstants.RANK_SCORE, String.valueOf(userId));
        log.debug("【查询排名】userId={}, 排名下标={}", userId, rank);
        // 未上榜返回 null
        return rank == null ? null : rank + 1;
    }

    /**
     * 查询用户积分：ZSCORE rank:score user:1
     */
    public Double getScore(Long userId) {
        Double score = stringRedisTemplate.opsForZSet().score(RedisKeyConstants.RANK_SCORE, String.valueOf(userId));
        log.debug("【查询积分】userId={}, 积分={}", userId, score);
        return score;
    }

    /**
     * 获取排行榜 TopN：ZREVRANGE rank:score 0 N-1
     * <p>
     * ZREVRANGE 反序取成员（分数从高到低），返回「前N名」。
     */
    public List<String> getTopN(int topN) {
        // 反序取前 N 个成员（分数从高到低）
        Set<String> topMembers = stringRedisTemplate.opsForZSet().reverseRange(RedisKeyConstants.RANK_SCORE, 0, topN - 1);
        List<String> topList = topMembers == null ? List.of() : topMembers.stream().toList();
        log.debug("【Top{}】= {}", topN, topList);
        return topList;
    }

    /**
     * 获取榜单（含分数）：ZREVRANGE WITHSCORES
     * <p>
     * 实际项目榜单要展示「排名+用户+分数」，使用 reverseRangeWithScores 一次取出。
     */
    public List<ZSetOperations.TypedTuple<String>> getTopNWithScores(int topN) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                stringRedisTemplate.opsForZSet().reverseRangeWithScores(RedisKeyConstants.RANK_SCORE, 0, topN - 1);
        List<ZSetOperations.TypedTuple<String>> result = tuples == null ? List.of() : tuples.stream().collect(Collectors.toList());
        log.debug("【Top{} 含分数】={}", topN, result.stream()
                .map(t -> t.getValue() + ":" + t.getScore())
                .collect(Collectors.joining(", ")));
        return result;
    }
}