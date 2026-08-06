package com.dev.redisspringpra.constant;

/**
 * Redis Key 设计规范
 * <p>
 * 企业级 Key 命名规范：业务模块:业务类型:ID
 * 例如：user:info:1001 表示「用户模块-用户信息-用户ID=1001」
 * <p>
 * 为什么统一管理 Key？
 * 1. 避免散落在代码中，后期修改前缀全局可控；
 * 2. 统一风格，便于使用 redis-cli 按前缀批量查询（KEYS user:*）。
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    /** ========== 用户模块 ========== */
    /** 用户信息缓存：user:info:{userId} */
    public static final String USER_INFO = "user:info:";

    /** 登录 Token：user:token:{token} */
    public static final String USER_TOKEN = "user:token:";

    /** 验证码：user:captcha:{phone} */
    public static final String USER_CAPTCHA = "user:captcha:";

    /** 点赞集合：like:product:{productId} */
    public static final String LIKE_PRODUCT = "like:product:";

    /** 用户点赞记录：like:user:{userId} */
    public static final String LIKE_USER = "like:user:";

    /** ========== 商品模块 ========== */
    /** 商品详情缓存：product:detail:{productId} */
    public static final String PRODUCT_DETAIL = "product:detail:";

    /** ========== 排行榜模块 ========== */
    /** 积分排行榜：rank:score */
    public static final String RANK_SCORE = "rank:score";

    /** ========== 限流模块 ========== */
    /** 接口限流：rate:limit:{ip} */
    public static final String RATE_LIMIT = "rate:limit:";

    /** ========== 消息模块 ========== */
    /** 消息通知频道（Pub/Sub） */
    public static final String CHANNEL_NOTICE = "channel:notice";

    /** ========== 库存模块（分布式锁） ========== */
    /** 库存扣减分布式锁：lock:stock:deduct */
    public static final String LOCK_STOCK_DEDUCT = "lock:stock:deduct";

    /** 商品库存 key：stock:product:{productId} */
    public static final String STOCK_PRODUCT = "stock:product:";
}