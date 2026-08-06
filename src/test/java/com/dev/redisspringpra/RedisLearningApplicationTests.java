package com.dev.redisspringpra;

import com.dev.redisspringpra.entity.Product;
import com.dev.redisspringpra.entity.User;
import com.dev.redisspringpra.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 学习案例集成测试
 * <p>
 * 运行前提：本地 Redis 已启动（redis-server）。
 * <p>
 * 覆盖：
 * 案例1 用户信息缓存（String）
 * 案例2 商品详情缓存（Hash）
 * 案例3 点赞统计（Set）
 * 案例4 排行榜（ZSet）
 * 案例5 验证码（String + TTL）
 * 案例6 登录Token（String + TTL）
 * 案例7 接口限流（INCR + EXPIRE）
 * 案例8 缓存穿透防护
 * 案例9 分布式锁（Redisson）
 * 案例10 消息通知（Pub/Sub）
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisLearningApplicationTests {

    @Autowired
    private UserCacheService userCacheService;

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private RankService rankService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private CachePenetrationService cachePenetrationService;

    @Autowired
    private StockService stockService;

    @Autowired
    private NoticePublisher noticePublisher;

    @Autowired
    private SpringCacheService springCacheService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 每个测试前清理相关 Redis key，保证测试可重复执行
     */
    @BeforeEach
    void cleanKeys() {
        // 清空本案例使用的所有 key（保留 Redis 其他数据）
        stringRedisTemplate.delete(java.util.List.of(
                "user:info:1",
                "product:detail:1",
                "like:product:1",
                "like:product:2",
                "rank:score",
                "user:captcha:13900000001",
                "user:captcha:13900000001:rate",
                "user:captcha:13900000001x",
                "user:captcha:13900000001x:rate",
                "user:token:invalid-token",
                "rate:limit:192.168.1.100",
                "product:detail:999",
                "stock:product:3",
                "lock:stock:deduct:3",
                "bloom:user"
        ));
        // 清空 Spring Cache 的 user 缓存区
        stringRedisTemplate.delete("user::1");
    }

    /** ================= 案例1：用户信息缓存 ================= */

    @Test
    @Order(1)
    @DisplayName("案例1：用户信息缓存 - 查询/更新/删除")
    void testUserCache() {
        // 1. 首次查询：查数据库 + 写缓存
        User user1 = userCacheService.getUserById(1L);
        assertNotNull(user1);
        assertEquals("用户1", user1.getName());

        // 2. 再次查询：缓存命中
        User user2 = userCacheService.getUserById(1L);
        assertNotNull(user2);

        // 3. 更新用户：先更新DB后删缓存
        User updated = userCacheService.updateUser(1L, "新名字", 30);
        assertEquals("新名字", updated.getName());

        // 4. 更新后查询：返回新数据（缓存已删，重新查库）
        User user3 = userCacheService.getUserById(1L);
        assertEquals("新名字", user3.getName());

        // 5. TTL 演示
        assertNotNull(userCacheService.getCacheTtl(1L));
    }

    /** ================= 案例2：商品详情缓存 ================= */

    @Test
    @Order(2)
    @DisplayName("案例2：商品详情缓存 - Hash 查询/字段更新/动态扩展")
    void testProductCache() {
        // 1. 首次查询商品
        Product product = productCacheService.getProductById(1L);
        assertNotNull(product);
        assertEquals("iPhone 16 Pro", product.getName());

        // 2. 更新价格（字段级更新）
        Product updated = productCacheService.updateProduct(1L, null, new BigDecimal("7999.00"), null);
        assertEquals(0, new BigDecimal("7999.00").compareTo(updated.getPrice()));

        // 3. 再次查询：从 Hash 缓存读取，价格应更新
        Product fromCache = productCacheService.getProductById(1L);
        assertEquals(0, new BigDecimal("7999.00").compareTo(fromCache.getPrice()));

        // 4. 动态扩展字段（tags）
        productCacheService.addProductTag(1L, "旗舰,新品");
    }

    /** ================= 案例3：点赞统计 ================= */

    @Test
    @Order(3)
    @DisplayName("案例3：点赞 - 点赞/取消/判断/统计/交集")
    void testLike() {
        // 1. 用户1、2、3 点赞商品1
        assertTrue(likeService.like(1L, 1L));
        assertTrue(likeService.like(1L, 2L));
        assertTrue(likeService.like(1L, 3L));

        // 2. 重复点赞失败
        assertFalse(likeService.like(1L, 1L));

        // 3. 判断已赞
        assertTrue(likeService.isLiked(1L, 1L));
        assertFalse(likeService.isLiked(1L, 99L));

        // 4. 统计 = 3
        assertEquals(3L, likeService.likeCount(1L));

        // 5. 取消点赞
        assertTrue(likeService.unlike(1L, 1L));
        assertEquals(2L, likeService.likeCount(1L));

        // 6. 商品2 让用户1、2点赞 -> 与商品1 交集 = 用户2
        likeService.like(2L, 1L);
        likeService.like(2L, 2L);
        assertEquals(1, likeService.commonLikers(1L, 2L).size());
    }

    /** ================= 案例4：排行榜 ================= */

    @Test
    @Order(4)
    @DisplayName("案例4：排行榜 - 加分/排名/TopN")
    void testRank() {
        // 1. 给用户加分
        rankService.addScore(1L, 100);
        rankService.addScore(2L, 50);
        rankService.addScore(3L, 200);

        // 2. 查询积分
        assertEquals(100.0, rankService.getScore(1L));
        assertEquals(3, rankService.getTopN(10).size());

        // 3. 排名：用户3最高（200分）排第1，用户1（100分）第2，用户2（50分）第3
        Long rank1 = rankService.getRank(1L);
        Long rank2 = rankService.getRank(2L);
        Long rank3 = rankService.getRank(3L);
        assertEquals(2L, rank1);
        assertEquals(3L, rank2);
        assertEquals(1L, rank3);
    }

    /** ================= 案例5：验证码 ================= */

    @Test
    @Order(5)
    @DisplayName("案例5：验证码 - 生成/校验/TTL")
    void testCaptcha() {
        String phone = "13900000001";

        // 1. 发送验证码
        String captcha = captchaService.sendCaptcha(phone);
        assertNotNull(captcha);
        assertEquals(6, captcha.length());

        // 2. 校验正确验证码
        assertTrue(captchaService.verifyCaptcha(phone, captcha));

        // 3. 校验后已被消费（一次性）
        assertThrows(Exception.class, () -> captchaService.verifyCaptcha(phone, captcha));

        // 4. 防刷：同一手机号 60 秒内重复发送应报错
        assertThrows(Exception.class, () -> captchaService.sendCaptcha(phone));
    }

    /** ================= 案例6：登录Token ================= */

    @Test
    @Order(6)
    @DisplayName("案例6：Token - 登录/校验/退出")
    void testToken() {
        // 1. 登录
        String token = tokenService.login(1L);
        assertNotNull(token);

        // 2. 校验通过
        User user = tokenService.verifyToken(token);
        assertNotNull(user);
        assertEquals(1L, user.getId());

        // 3. 错误 token 抛异常
        assertThrows(Exception.class, () -> tokenService.verifyToken("invalid-token"));

        // 4. 退出登录
        tokenService.logout(token);
        // 退出后 token 失效
        assertThrows(Exception.class, () -> tokenService.verifyToken(token));
    }

    /** ================= 案例7：接口限流 ================= */

    @Test
    @Order(7)
    @DisplayName("案例7：限流 - 超过阈值被限流")
    void testRateLimit() {
        String ip = "192.168.1.100";

        // 1. 前面 10 次正常（配置 max-requests=10）
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.tryAcquire(ip));
        }

        // 2. 第 11 次触发限流
        assertThrows(Exception.class, () -> rateLimitService.tryAcquire(ip));
    }

    /** ================= 案例8：缓存穿透防护 ================= */

    @Test
    @Order(8)
    @DisplayName("案例8：缓存穿透 - 空对象 + 布隆过滤器思想")
    void testPenetration() {
        // 1. 查询不存在的商品 999 -> 返回 null + 写入空对象
        assertNull(cachePenetrationService.getProductWithProtection(999L));

        // 2. 再次查询：命中空对象缓存，不再查 DB（仍返回 null）
        assertNull(cachePenetrationService.getProductWithProtection(999L));

        // 3. 布隆过滤器：未注册的用户被拦截
        assertThrows(Exception.class, () -> cachePenetrationService.getUserWithBloomFilter(999L));

        // 4. 注册用户 1 后正常查询
        cachePenetrationService.registerUserId(1L);
        User user = cachePenetrationService.getUserWithBloomFilter(1L);
        assertNotNull(user);
    }

    /** ================= 案例9：分布式锁（秒杀） ================= */

    @Test
    @Order(9)
    @DisplayName("案例9：分布式锁 - 秒杀库存扣减")
    void testSeckill() {
        // 1. 初始化库存 5 件（使用 MockDb 预置的商品 3）
        stockService.initStock(3L, 5);
        assertEquals(5L, stockService.getStock(3L));

        // 2. 发起 100 个并发秒杀请求
        int successCount = 0;
        for (long userId = 1; userId <= 100; userId++) {
            final long uid = userId;
            try {
                if (stockService.seckill(3L, uid)) {
                    successCount++;
                }
            } catch (Exception ignore) {
                // 并发失败会被拒绝（限流/库存不足）
            }
        }

        // 3. 最多 5 人抢到（库存不被超卖）
        assertTrue(successCount <= 5, "秒杀成功数不能超过库存：" + successCount);
        assertEquals(0L, stockService.getStock(3L));
        System.out.println("秒杀成功数 = " + successCount);
    }

    /** ================= 案例10：消息通知 ================= */

    @Test
    @Order(10)
    @DisplayName("案例10：Pub/Sub - 发布通知")
    void testNotice() {
        // 发布通知（订阅者是异步的，发布后 sleep 等待订阅线程回调）
        noticePublisher.publish("订单20240101001已发货");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 观察控制台：【收到通知】频道=channel:notice, 内容=订单20240101001已发货
    }

    /** ================= 第三阶段：Spring Cache ================= */

    @Test
    @Order(11)
    @DisplayName("第三阶段：Spring Cache - @Cacheable/@CachePut/@CacheEvict")
    void testSpringCache() {
        // 1. @Cacheable：首次查库，写入缓存
        User user1 = springCacheService.getUserWithCache(1L);
        assertNotNull(user1);

        // 2. @CachePut：更新DB并刷新缓存
        User updated = springCacheService.updateUserWithCache(1L, "SpringCache用户", 25);
        assertEquals("SpringCache用户", updated.getName());

        // 3. @Cacheable：再次读取（命中缓存，拿到最新数据）
        User cached = springCacheService.getUserWithCache(1L);
        assertEquals("SpringCache用户", cached.getName());

        // 4. @CacheEvict：删除缓存
        springCacheService.evictUserCache(1L);
    }
}