# Redis 学习笔记（Spring Boot 实战版）

> 配套项目：redisSpringPra（Spring Boot 3.4 + Java 21 + RedisTemplate/Redisson）
> 学习方法：索引式学习 —— 先建知识地图，再深入细节，最后形成记忆锚点

---

## 知识地图（索引式学习）

```
Redis 学习路线
├── L1 基础（必须掌握）
│   ├── Redis 是什么 / NoSQL 分类 / 与 MySQL 区别
│   ├── Redis 为什么快
│   └── 常用命令 + 五种数据结构
├── L2 Spring Boot 整合（必须掌握）
│   ├── RedisTemplate / StringRedisTemplate
│   ├── 序列化（String + JSON）
│   └── Spring Cache（@Cacheable / @CachePut / @CacheEvict）
├── L3 业务案例（必须掌握）
│   ├── 缓存（String/Hash）→ 点赞（Set）→ 排行榜（ZSet）
│   ├── 验证码/Token/限流（String + TTL）
│   └── 穿透防护 / 分布式锁 / Pub/Sub
├── L4 面试专题（L1-L2 内容深度展开）
│   ├── 穿透/击穿/雪崩/双写一致性
│   ├── RDB/AOF/高可用
│   └── 事务
└── L5 高级特性（扩展）
    ├── Bitmap / HyperLogLog / GEO / Stream / ACL
    └── Redisson 高级用法
```

---

# 第一阶段 Redis 基础

## 1. Redis 是什么

### 1. 一句话核心版
Redis 是**基于内存的键值型 NoSQL 数据库**，解决「高速读写 + 缓存 + 分布式场景」问题，常用于缓存、会话、排行榜、消息。

### 2. 核心原理
```
请求 → Redis（内存） → 命中/未命中 → 业务处理/回源 DB
```
- 为什么快：纯内存操作 + 单线程（避免上下文切换/锁竞争）+ IO 多路复用
- Redis 6 多线程：**网络 IO 多线程，命令执行仍单线程**（保证原子性）

### 3. Java 后端视角
- 项目位置：`config/RedisConfig.java`、业务数据在 MySQL（`repository/` 下 JPA Repository + DataInitializer 种子数据）
- Key 设计：`业务模块:业务类型:ID`（见 `constant/RedisKeyConstants.java`）
- 为什么不用 MySQL：Redis 单线程 10万+ QPS，MySQL 磁盘 IO 远达不到（MySQL 负责持久化，Redis 做缓存加速）

### 4. 常见命令
| 命令 | 作用 | RedisTemplate |
|------|------|--------------|
| SET/GET | 写入/读取 | opsForValue().set/get |
| INCR/DECR | 原子增减 | opsForValue().increment/decrement |
| EXPIRE | 过期时间 | expire(key, ttl) |
| DEL | 删除 | delete(key) |

### 5. 执行结果
`redis-cli` 输入 `SET user:info:1 张三` → OK；`GET user:info:1` → 张三

### 6. 注意事项
- Key 要有统一前缀，方便按模块排查
- 大 key 慎用 KEYS 命令（阻塞），用 SCAN

### 7. 面试回答
**问：Redis 为什么快？**
答：三个原因：①纯内存操作，数据读写都在内存完成；②单线程模型，避免了多线程上下文切换和锁竞争的开销；③IO 多路复用 + 非阻塞 IO，一个线程可以同时处理大量连接。Redis 6 之后网络 IO 改为多线程，但命令执行仍是单线程，保证原子性。

---

# 第二阶段 五种数据结构

| 结构 | 底层实现 | 适用场景 | 项目案例 |
|------|---------|---------|---------|
| String | SDS 动态字符串 | 缓存/计数器/Token/验证码 | 案例1/5/6/7 |
| Hash | 哈希表 + 压缩列表 | 对象字段级读写 | 案例2 |
| List | 双向链表/快速列表 | 消息队列、最新列表 | 扩展 |
| Set | 哈希表 + intset | 点赞/标签/共同好友 | 案例3 |
| ZSet | 跳表 + 哈希表 | 排行榜 | 案例4 |

## 面试回答（数据结构选型）
**问：点赞为什么用 Set？**
答：点赞的核心是「点赞/取消/判断是否已赞/统计数量」，Set 天然支持 SADD、SREM、SISMEMBER(O(1))、SCARD(O(1))。用 String 只能计数无法判断谁点的、无法取消；用 List 无法去重。

**问：排行榜为什么用 ZSet？**
答：ZSet = Set + 分数，ZINCRBY 原子加分、ZREVRANGE 取 TopN、ZREVRANK 查排名，一次命令完成排序需求。其他结构需要自己维护排序，插入删除成本高。

---

# 第三阶段 Spring Boot 整合

## RedisTemplate vs StringRedisTemplate

| 对比 | RedisTemplate | StringRedisTemplate |
|------|--------------|-------------------|
| Value 序列化 | JSON（带类型信息） | 纯字符串 |
| 适用 | 存对象（User/Product） | 存纯字符串（计数/Token/Set成员） |
| 可读性 | redis-cli 可见 JSON | 完全可读 |

**面试点：为什么 key 用 String、value 用 JSON？**
- String key：redis-cli 可读、方便排查
- JSON value：体积小、可跨语言

## Spring Cache 三大注解

| 注解          | 作用                  | 项目位置                                     |
|-------------|---------------------|------------------------------------------|
| @Cacheable  | 查询：先查缓存，未命中执行方法并写缓存 | `SpringCacheService.getUserWithCache`    |
| @CachePut   | 更新：执行方法后把返回值写缓存     | `SpringCacheService.updateUserWithCache` |
| @CacheEvict | 删除：执行方法后删缓存         | `SpringCacheService.evictUserCache`      |

**面试点：注解 vs 手写 RedisTemplate？**
- 注解：适合缓存逻辑统一的场景，开发快
- 手写：适合复杂场景（分布式锁+缓存重建、双删），可控性强

---

# 第四阶段 业务案例（代码都在项目中，含详细中文注释）

## 案例1：用户信息缓存（String + Cache Aside）
- **代码**：`service/UserCacheService.java`、`controller/UserCacheController.java`
- **核心流程**：查缓存 → 未命中查 DB → 写缓存；更新：先 DB 后删缓存
- **为什么删缓存不更新缓存**：删缓存更简单，下次查询重建自然拿到最新数据（延迟加载）
- **测试**：GET/PUT/DELETE `/api/user/{id}`

### 面试回答
**问：Cache Aside 是什么？先更新 DB 还是先删缓存？**
答：旁路缓存模式。读：先查缓存，未命中查库写缓存；写：先更新数据库，再删除缓存。只删缓存不更新缓存，是为了避免并发写覆盖问题。极端一致场景（读写并发）可用延迟双删：先删缓存 → 更新 DB → sleep 500ms → 再删一次缓存。

## 案例2：商品详情缓存（Hash）
- **代码**：`service/ProductCacheService.java`、`controller/ProductCacheController.java`
- **为什么用 Hash**：字段级读写，改价格只 HSET price 字段，不需要整体反序列化重写
- **换 String 的问题**：更新一个字段要 GET → 反序列化 → 改 → SET，并发下易覆盖丢失更新

## 案例3：点赞统计（Set）
- **代码**：`service/LikeService.java`、`controller/LikeController.java`
- **API**：SADD(like) / SREM(unlike) / SISMEMBER(isLiked) / SCARD(count) / SINTER(共同点赞)

## 案例4：排行榜（ZSet）
- **代码**：`service/RankService.java`、`controller/RankController.java`
- **API**：ZINCRBY(加分) / ZREVRANK(排名) / ZSCORE(查分) / ZREVRANGE(TopN)

## 案例5：验证码（String + TTL）
- **代码**：`service/CaptchaService.java`、`controller/CaptchaController.java`
- **要点**：SETEX 写入+过期一步完成；校验后 DEL（一次性）；SETNX 防刷（60秒限一次）
- **为什么不用 DB**：验证码高频过期清理，DB 无 TTL 机制

## 案例6：登录 Token（String + TTL + 滑动续期）
- **代码**：`service/TokenService.java`、`controller/TokenController.java`
- **要点**：登录 SET token→userId 带 30 分钟 TTL；每次校验成功 EXPIRE 重置 TTL（滑动过期）；退出 DEL 主动失效

## 案例7：接口限流（INCR + EXPIRE）
- **代码**：`service/RateLimitService.java`、`controller/RateLimitController.java`
- **原理**：INCR 计数，第一次设置 EXPIRE 窗口，超阈值拒绝（固定窗口算法）
- **坑**：只第一次设置 EXPIRE，否则变成滑动窗口导致限流失效

## 案例8：缓存穿透防护（空对象 + 布隆过滤器思想）
- **代码**：`service/CachePenetrationService.java`、`controller/CachePenetrationController.java`
- **空对象**：查不到也缓存 EMPTY（3分钟 TTL），后续同 key 请求不再打 DB
- **布隆思想**：查询前先 SISMEMBER 判断 id 是否存在，不存在直接拒绝

### 面试回答（三大缓存问题）
**问：缓存穿透/击穿/雪崩的区别和解决？**
- **穿透**：查不存在的数据 → 缓存空对象 + 布隆过滤器
- **击穿**：热点 key 过期瞬间大量请求打 DB → 互斥锁 + 逻辑过期
- **雪崩**：大量 key 同时过期 → 过期时间加随机值 + 多级缓存

## 案例9：分布式锁（Redisson）
- **代码**：`service/StockService.java`、`controller/StockController.java`、`config/RedissonConfig.java`
- **为什么用 Redisson 不用手写 SETNX**：看门狗自动续期（防锁过期）、Lua 原子校验持有者（防误删）、可重入
- **秒杀流程**：DECR 预扣 Redis 库存 → tryLock 分布式锁 → MySQL 原子扣减（UPDATE ... WHERE stock > 0）→ 释放锁

### 面试回答
**问：Redisson 分布式锁的原理？**
答：基于 SET NX EX 的 Lua 脚本实现加锁原子性，value 存线程 ID 用于可重入和持有者校验。看门狗线程默认每 10 秒把锁续期到 30 秒，防止业务没执行完锁就过期。释放锁用 Lua 校验持有者是当前线程才 DEL，避免误删别人的锁。

## 案例10：消息通知（Pub/Sub）
- **代码**：`service/NoticePublisher.java`、`listener/NoticeSubscriber.java`、`config/RedisPubSubConfig.java`
- **流程**：发布者 convertAndSend → PUBLISH 频道 → 订阅者回调 onMessage
- **缺点（面试重点）**：消息不持久化，订阅者不在线消息丢失；可靠消息用 Stream/MQ

---

# 第五阶段 面试专题速记

## 双写一致性（延迟双删）
```
更新 DB → 删除缓存 → sleep(500ms) → 再删除缓存
```
为什么 500ms：等待读请求把旧缓存重建完成的时间窗口

## 持久化
| 方式 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| RDB | 快照 | 恢复快文件小 | 可能丢最后几分钟数据 |
| AOF | 追加日志 | 最多丢 1 秒 | 文件大恢复慢 |
| Redis7 混合 | AOF 重写时用 RDB 头 | 兼顾恢复速度和低丢失 | — |

## 高可用
主从复制（数据冗余）→ Sentinel（故障自动切换）→ Cluster（分片扩容）

## 事务
**问：Redis 事务为什么不能回滚？**
答：①Redis 单线程，事务中命令要么都执行要么都执行前的语法错误才不执行；②运行时错误（如对 String 用 LPUSH）不会回滚前面已执行的命令；③设计上认为开发者错误不应由 Redis 负责，简化实现。

---

# 第六阶段 高级特性（扩展索引）

| 特性 | 一句话 | 场景 |
|------|-------|------|
| Bitmap | 位图，1 bit 存一个状态 | 签到、用户在线状态 |
| HyperLogLog | 基数统计，12KB 存 2^64 | UV 统计 |
| GEO | 经纬度存储与距离计算 | 附近的人 |
| Stream | 持久化消息队列 | 可靠消息 |
| ACL | 用户权限控制 | 多团队共享 Redis |

---

# 面试总纲（1 分钟速记）

1. **为什么快**：内存 + 单线程 + IO 多路复用
2. **数据结构**：场景导向 —— 缓存 String/Hash、点赞 Set、榜单 ZSet
3. **缓存三大问题**：穿透（空对象+布隆）、击穿（互斥锁）、雪崩（随机 TTL）
4. **一致性**：Cache Aside + 延迟双删
5. **分布式锁**：Redisson 看门狗自动续期 + Lua 原子
6. **持久化**：RDB 快、AOF 稳
7. **高可用**：主从 / Sentinel / Cluster

---

# 项目运行 & 测试

```bash
# 1. 启动 Redis（如未启动）
redis-server

# 2. 初始化 MySQL（只需首次，root 密码按本机修改）
mysql -uroot -p你的密码 < sql/init.sql
# 或只建库：mysql -uroot -p你的密码 -e "CREATE DATABASE IF NOT EXISTS redis_spring_pra DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;"
# 表结构由 JPA ddl-auto=update 启动自动创建，表为空自动插入 5 用户 + 3 商品种子数据

# 3. 运行全部案例测试（11 个用例）
JAVA_HOME=/Users/unravel/Library/Java/JavaVirtualMachines/ms-21.0.12/Contents/Home mvn test

# 4. 启动应用（方便 curl 测试 REST 接口）
JAVA_HOME=/Users/unravel/Library/Java/JavaVirtualMachines/ms-21.0.12/Contents/Home mvn spring-boot:run
```

**接口速查**（启动后 8080 端口）：
| 接口 | 案例 |
|------|------|
| GET/PUT/DELETE `/api/user/{id}` | 用户缓存 |
| GET/PUT `/api/product/{id}` | 商品 Hash |
| POST/DELETE `/api/like/{pid}/{uid}` | 点赞 |
| POST `/api/rank/{uid}/score?points=10` | 排行榜 |
| POST `/api/captcha/send?phone=xxx` | 验证码 |
| POST `/api/token/login?userId=1` | Token |
| GET `/api/rate-limit/test` | 限流 |
| POST `/api/stock/3/seckill?userId=1` | 秒杀/分布式锁 |
| POST `/api/notice/publish?message=xx` | Pub/Sub |
| GET `/api/cache/user/1` | Spring Cache |