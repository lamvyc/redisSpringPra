# redisSpringPra · Redis 学习实战项目

一个 **Spring Boot 3.4 + Java 21 + Redis** 的学习工程，通过 10 个企业级业务案例系统掌握 Redis。

> 配套文档：`docs/Redis学习笔记.md`（知识地图 + 7 步知识点 + 面试专题）

---

## 📌 技术栈

| 技术                | 版本        | 用途                                                 |
|-------------------|-----------|----------------------------------------------------|
| Spring Boot       | 3.4.3     | 应用框架                                               |
| Java              | 21        | 开发语言                                               |
| Spring Data Redis | 随 Boot 管理 | RedisTemplate / StringRedisTemplate / Spring Cache |
| Redisson          | 3.38.1    | 分布式锁                                               |
| Lombok            | 随 Boot 管理 | 简化实体代码                                             |
| Redis             | 7.x（本机）   | 缓存 / 数据结构 / Pub/Sub                                |

---

## 🚀 初始化项目（重新 clone 后）

### 1. 环境要求

| 依赖    | 版本   | 验证命令                     |
|-------|------|--------------------------|
| JDK   | 21   | `java -version`          |
| Maven | 3.9+ | `mvn -version`           |
| Redis | 5.0+ | `redis-server --version` |

### 2. 获取代码

```bash
git clone <你的仓库地址> redis-learning
cd redis-learning
```

### 3. 启动 Redis

```bash
# 方式一：前台启动（终端保持打开）
redis-server

# 方式二：后台启动
redis-server --daemonize yes

# 验证是否启动成功
redis-cli ping    # 返回 PONG 即成功
```

<details>
<summary>Mac 没有安装 Redis？用 Homebrew 安装</summary>

```bash
brew install redis
redis-server
```
</details>

### 4. 编译项目

```bash
# 使用 JDK 21（按本机实际 JDK 21 路径调整）
export JAVA_HOME=/Users/unravel/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home

# 下载依赖并编译
mvn clean compile
```

> ⚠️ 注意：Maven 默认使用的 JDK 版本需 ≥ 21。如果本机默认 JDK 不是 21，请用 `JAVA_HOME` 指定 JDK 21。

---

## ▶️ 启动项目

### 方式一：运行全部案例测试（推荐先做这个）

```bash
export JAVA_HOME=/Users/unravel/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home
mvn test
```

测试共 **11 个用例**，覆盖全部 10 个案例 + Spring Cache，输出 `BUILD SUCCESS` 即全部通过。

### 方式二：启动 Web 服务（REST 接口测试）

```bash
export JAVA_HOME=/Users/unravel/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home
mvn spring-boot:run
```

启动后访问：http://localhost:8080

#### 接口速查表

| 案例              | 方法 & 路径                                            | 说明                |
|-----------------|----------------------------------------------------|-------------------|
| **1 用户缓存**      | `GET /api/user/1`                                  | 查询用户（首查写缓存）       |
|                 | `PUT /api/user/1` + Body `{"name":"新名字","age":30}` | 修改用户（先DB后删缓存）     |
|                 | `DELETE /api/user/1/cache`                         | 删除用户缓存            |
| **2 商品缓存**      | `GET /api/product/1`                               | Hash 查询商品         |
|                 | `PUT /api/product/1` + Body `{"price":7999}`       | 字段级更新价格           |
|                 | `POST /api/product/1/tag?tag=旗舰`                   | Hash 动态加字段        |
| **3 点赞**        | `POST /api/like/1/1`                               | 用户1 点赞商品1         |
|                 | `GET /api/like/1/count`                            | 点赞统计              |
|                 | `GET /api/like/1/common?productId2=2`              | 两个商品共同点赞用户        |
| **4 排行榜**       | `POST /api/rank/1/score?points=100`                | 用户1 加 100 分       |
|                 | `GET /api/rank/top?n=10`                           | Top10 榜单          |
| **5 验证码**       | `POST /api/captcha/send?phone=13800000001`         | 发验证码（直接返回）        |
|                 | `POST /api/captcha/verify?phone=xxx&code=123456`   | 校验验证码             |
| **6 Token**     | `POST /api/token/login?userId=1`                   | 登录拿 Token         |
|                 | `GET /api/token/verify?token=xxx`                  | 校验 Token（自动续期）    |
| **7 限流**        | `GET /api/rate-limit/test`                         | 连刷 11 次触发限流(429)  |
| **8 穿透**        | `GET /api/penetration/product/999`                 | 空对象防护（不存在商品）      |
|                 | `GET /api/penetration/user/999`                    | 布隆过滤器拦截           |
| **9 秒杀**        | `POST /api/stock/3/init?stock=10`                  | 初始化库存             |
|                 | `POST /api/stock/3/seckill?userId=1`               | 秒杀（Redisson 分布式锁） |
|                 | `GET /api/stock/3`                                 | 查询剩余库存            |
| **10 通知**       | `POST /api/notice/publish?message=订单已发货`           | 发布通知（订阅者实时收到）     |
| **SpringCache** | `GET /api/cache/user/1`                            | @Cacheable 查询     |
|                 | `PUT /api/cache/user/1`                            | @CachePut 更新+刷新缓存 |

#### curl 秒杀并发测试（演示防超卖）

```bash
# 终端1：初始化库存 10 件
curl -X POST "http://localhost:8080/api/stock/3/init?stock=10"

# 终端2：并发发起 50 个秒杀请求（只有 10 个能成功）
seq 1 50 | xargs -P 10 -I {} curl -s -o /dev/null -w "%{http_code} " -X POST "http://localhost:8080/api/stock/3/seckill?userId={}"

# 查看剩余库存（应为 0，且不会超卖为负数）
curl "http://localhost:8080/api/stock/3"
```

---

## 📖 当前项目阅读学习顺序（按文件逐个读）

> 核心方法：**从入口到案例，从不变量到变化量**。
> 先搭骨架（配置层），再按「简单 → 复杂」读 10+1 个业务案例。
> 每个案例固定读 3 类文件：`Service`（业务逻辑+为什么）→ `Controller`（接口入口）→ 测试用例（验证结果）。
> 所有 `Service` 的类注释里都有「为什么选这个数据结构 / 换其他结构有什么问题」，这是本项目最重要的学习材料。

### 第一步：读骨架（5 个文件，10 分钟）

| 顺序 | 文件                                   | 读什么                  | 回答什么问题             |
|----|--------------------------------------|----------------------|--------------------|
| 1  | `pom.xml`                            | 5 个依赖                | 项目用了哪些技术栈？         |
| 2  | `src/main/resources/application.yml` | Redis 连接、缓存 TTL、限流参数 | 配置如何与代码关联？         |
| 3  | `RedisSpringPraApplication.java`     | 启动类                  | 项目入口               |
| 4  | `constant/RedisKeyConstants.java`    | 所有 Key 前缀            | Redis Key 命名规范是什么？ |
| 5  | `repository/MockDb.java`             | 预置用户/商品数据            | 数据从哪来？（模拟 MySQL）   |

### 第二步：读公共层（5 个文件，15 分钟）

| 顺序 | 文件                                                         | 读什么                  | 回答什么问题                                   |
|----|------------------------------------------------------------|----------------------|------------------------------------------|
| 6  | `common/Result.java`                                       | 统一返回结构               | 接口返回格式                                   |
| 7  | `common/BizException.java` + `GlobalExceptionHandler.java` | 异常处理                 | 业务错误如何规范化返回？                             |
| 8  | `config/AppProperties.java`                                | 配置绑定                 | 限流/验证码参数如何读取？                            |
| 9  | `config/RedisConfig.java`                                  | 序列化配置 + CacheManager | **为什么 key 用 String、value 用 JSON？**（面试必问） |
| 10 | `config/RedissonConfig.java`                               | 分布式锁客户端              | Redisson 如何连接？                           |

### 第三步：按案例顺序读业务代码（核心，每个案例 5-10 分钟）

> 每个案例的阅读顺序固定：**先 Service（核心）→ 再 Controller（接口）→ 最后测试用例（验证）**。
> 案例按「简单 → 复杂」排列，后一个案例会用到前面案例的玩法。

| 阅读顺序 | 案例           | 读 Service                                                                                           | 读 Controller                                 | 跑测试                | 这一案例学什么                                               |
|------|--------------|-----------------------------------------------------------------------------------------------------|----------------------------------------------|--------------------|-------------------------------------------------------|
| ①    | 案例1 用户缓存     | `service/UserCacheService.java`                                                                     | `controller/UserCacheController.java`        | `testUserCache`    | **String + Cache Aside（最基础，必读）**：先查缓存→查DB→写缓存；先DB后删缓存 |
| ②    | 案例2 商品缓存     | `service/ProductCacheService.java`                                                                  | `controller/ProductCacheController.java`     | `testProductCache` | **Hash 数据结构**：字段级读写 HSET，为什么不用 String                 |
| ③    | 案例3 点赞       | `service/LikeService.java`                                                                          | `controller/LikeController.java`             | `testLike`         | **Set 数据结构**：SADD/SREM/SISMEMBER + 交集 SINTER          |
| ④    | 案例4 排行榜      | `service/RankService.java`                                                                          | `controller/RankController.java`             | `testRank`         | **ZSet 数据结构**：ZINCRBY/ZREVRANK/TopN                   |
| ⑤    | 案例5 验证码      | `service/CaptchaService.java`                                                                       | `controller/CaptchaController.java`          | `testCaptcha`      | **String + TTL**：SETEX 自动过期 + SETNX 防刷                |
| ⑥    | 案例6 Token    | `service/TokenService.java`                                                                         | `controller/TokenController.java`            | `testToken`        | **TTL 滑动续期**：每次请求重置过期时间                               |
| ⑦    | 案例7 限流       | `service/RateLimitService.java`                                                                     | `controller/RateLimitController.java`        | `testRateLimit`    | **INCR + EXPIRE 固定窗口限流**                              |
| ⑧    | 案例8 穿透防护     | `service/CachePenetrationService.java`                                                              | `controller/CachePenetrationController.java` | `testPenetration`  | **空对象缓存 + 布隆过滤器思想**                                   |
| ⑨    | 案例9 分布式锁     | `service/StockService.java`                                                                         | `controller/StockController.java`            | `testSeckill`      | **Redisson 分布式锁**：看门狗/防超卖（面试高频）                       |
| ⑩    | 案例10 消息通知    | `service/NoticePublisher.java` → `listener/NoticeSubscriber.java` → `config/RedisPubSubConfig.java` | `controller/NoticeController.java`           | `testNotice`       | **Pub/Sub**：发布→订阅→回调                                  |
| ⑪    | Spring Cache | `service/SpringCacheService.java`                                                                   | `controller/SpringCacheController.java`      | `testSpringCache`  | **@Cacheable / @CachePut / @CacheEvict 三大注解**         |

### 第四步：整体串联（10 分钟）

| 顺序 | 文件                                                                        | 读什么                                                             |
|----|---------------------------------------------------------------------------|-----------------------------------------------------------------|
| 最后 | `src/test/java/com/dev/redisspringpra/RedisLearningApplicationTests.java` | 通读 11 个用例，对照每个 Service 的方法，理解「测试清理 key → 调 Service → 断言结果」的完整闭环 |
| 最后 | `docs/Redis学习笔记.md`                                                       | 复习 7 步知识点 + 面试问答                                                |

### 每个 Service 的具体阅读方法（4 步）

1. **读类注释**：明确「为什么用这个数据结构 / 换其他结构有什么问题」（本项目核心学习点）
2. **读核心方法**：`getXxx`（读路径）`updateXxx`（写路径）`deleteXxx`（删除路径）
3. **读中文「为什么」注释**：例如 —— 为什么删缓存不更新缓存？为什么只有第一次请求才 EXPIRE？为什么先 DECR 再拿锁？
4. **跑对应测试**：观察控制台 `DEBUG` 日志输出，与注释中的流程预期对照

### 学习优先级建议

| 优先级 | 内容                                 | 建议投入        |
|-----|------------------------------------|-------------|
| ⭐⭐⭐ | 案例 1/2/5/6/7（String/Hash + TTL 基础） | 必须吃透        |
| ⭐⭐⭐ | 案例 3/4（Set/ZSet 结构选型）              | 必须吃透        |
| ⭐⭐⭐ | 案例 9（分布式锁，面试高频）                    | 必须吃透        |
| ⭐⭐  | 案例 8（缓存穿透，面试高频）                    | 理解原理        |
| ⭐⭐  | Spring Cache 注解                    | 会用即可        |
| ⭐   | 案例 10（Pub/Sub）                     | 了解即可，生产用 MQ |

### 进阶方向

| 阶段   | 目标               | 建议                                    |
|------|------------------|---------------------------------------|
| 当前项目 | 掌握 Redis 基础 + 实战 | 完成全部 11 个测试                           |
| 进阶 1 | 掌握缓存一致性          | 自己实现「延迟双删」并压测                         |
| 进阶 2 | 掌握 Redisson 高级   | 信号量/限流器/读写锁                           |
| 进阶 3 | 高级数据结构           | Bitmap 签到 / HyperLogLog UV / GEO 附近的人 |
| 进阶 4 | 生产高可用            | 主从复制 / Sentinel / Cluster 搭建          |
| 面试   | 表达输出             | 对着 docs 面试题录 1 分钟语音自查                 |

---

## 📁 项目结构速览

```
redis-learning/
├── pom.xml                          # Maven 配置（Web + Redis + Redisson + Lombok）
├── docs/
│   └── Redis学习笔记.md              # 📚 核心学习文档（知识地图/7步知识点/面试）
└── src/
    ├── main/java/com/dev/redisspringpra/
    │   ├── RedisSpringPraApplication.java   # 启动类
    │   ├── config/      # RedisConfig / RedissonConfig / RedisPubSubConfig / AppProperties
    │   ├── common/      # Result 统一返回 / BizException / 全局异常
    │   ├── constant/    # RedisKeyConstants（Key 规范）
    │   ├── entity/      # User / Product
    │   ├── repository/  # MockDb（内存 Map 模拟 MySQL）
    │   ├── dto/         # 请求参数
    │   ├── service/     # ★ 10 个业务案例（核心学习文件）
    │   ├── controller/  # REST 接口（测试入口）
    │   └── listener/    # Pub/Sub 订阅者
    ├── main/resources/
    │   └── application.yml   # Redis 连接 / 缓存配置 / 业务参数
    └── test/java/
        └── RedisLearningApplicationTests.java  # ★ 11 个用例集成测试
```

---

## ❓ 常见问题

**Q：测试报错连接不上 Redis？**
确保 Redis 已启动：`redis-cli ping` 返回 PONG。若 Redis 有密码，修改 `application.yml` 的 `spring.data.redis.password`。

**Q：`mvn test` 报 Lombok 编译错误？**
Maven 运行的 JDK 版本过高（如 25/26），改用 JDK 21：
```bash
export JAVA_HOME=/Users/unravel/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home
```

**Q：测试可以重复执行吗？**
可以。每个测试前会自动清理相关 Redis key（`@BeforeEach cleanKeys`）。

**Q：`rm -rf target` 后无法运行测试？**
重新 `mvn clean compile` 后再 `mvn test`。

---

## ✅ 最终能力目标（完成后自查）

- [ ] 能用 RedisTemplate / StringRedisTemplate 读写数据
- [ ] 能根据业务场景选择 String / Hash / Set / ZSet
- [ ] 能说出缓存穿透/击穿/雪崩的区别和解决方案
- [ ] 能解释 Cache Aside 和延迟双删
- [ ] 能讲清 Redisson 分布式锁的看门狗机制
- [ ] 能阅读企业 Redis 代码并审查 AI 生成的 Redis 代码
- [ ] 能 1 分钟内回答 Redis 高频面试题