# Redis 命令 ↔ Java API 破译表（本项目专用）

> 这份表专治「看到 `redisTemplate.opsForXxx()` 就懵」的问题。
> 核心认知一句话：**RedisTemplate 只是「发命令的遥控器」，每个 Java 方法名就是一条 Redis 命令的驼峰翻译。**

---

## 0. 先记住 3 条铁律

1. **Redis 只有 5 种数据结构**：String / Hash / List / Set / ZSet。
   Java 里用 `opsForXxx()` 选择「我要操作哪种结构」，之后的每个方法都是对这种结构的一条命令。

2. **方法名 = 命令的驼峰翻译**：
   - `increment` → `INCR`
   - `setIfAbsent` → `SET NX`（Set If Not eXists）
   - `isMember` → `SISMEMBER`
   - `intersect` → `SINTER`
   - `incrementScore` → `ZINCRBY`
   - `reverseRange` → `ZREVRANGE`
   - `getExpire` → `TTL`
   看懂方法名 ≈ 看懂命令。

3. **两个 Template 怎么选**：
   - `RedisTemplate<String, Object>`：存对象（User/Product），value 用 JSON 序列化。
   - `StringRedisTemplate`：存纯字符串/数字/集合成员，redis-cli 里更可读。
   > 存对象用前者，存验证码/计数/点赞成员/排行分数用后者。

---

## 1. 完整破译表

### ① String（`opsForValue()`）—— 一个 key 存一个值

| Java 方法                             | Redis 命令     | 含义          | 本项目出现处                      |
|-------------------------------------|--------------|-------------|-----------------------------|
| `get(key)`                          | `GET`        | 取值          | 几乎所有案例                      |
| `set(key, value)`                   | `SET`        | 存值（不设过期）    | StockService 初始化库存          |
| `set(key, value, Duration)`         | `SET ... EX` | 存值 + 设过期    | UserCache / Captcha / Token |
| `setIfAbsent(key, value, Duration)` | `SET NX EX`  | 不存在才写入（防刷）  | Captcha 60 秒防刷              |
| `increment(key)`                    | `INCR`       | 原子 +1（返回新值） | RateLimit / Stock 回补        |
| `decrement(key)`                    | `DECR`       | 原子 -1       | Stock 预扣库存                  |
| `hasKey(key)`                       | `EXISTS`     | key 是否存在    | ProductCache                |
| `expire(key, Duration)`             | `EXPIRE`     | 设置/重置过期     | Token 续期 / RateLimit 窗口     |
| `getExpire(key, TimeUnit)`          | `TTL`        | 查剩余过期秒数     | UserCache.getCacheTtl       |
| `delete(key)`                       | `DEL`        | 删除 key      | 所有删缓存场景                     |

### ② Hash（`opsForHash()`）—— 一个 key 里存「字段→值」表（类似 Java Map / 数据库一行）

| Java 方法                  | Redis 命令  | 含义       | 出现处                  |
|--------------------------|-----------|----------|----------------------|
| `put(key, field, value)` | `HSET`    | 写/改单个字段  | ProductCache 加标签     |
| `putAll(key, map)`       | `HMSET`   | 一次性写多个字段 | ProductCache 重建 / 更新 |
| `entries(key)`           | `HGETALL` | 取出全部字段   | ProductCache 查询      |
| `get(key, field)`        | `HGET`    | 取单个字段    | （扩展）                 |

### ③ Set（`opsForSet()`）—— 无序、元素唯一

| Java 方法                | Redis 命令    | 含义          | 出现处                     |
|------------------------|-------------|-------------|-------------------------|
| `add(key, value)`      | `SADD`      | 加成员（重复自动忽略） | LikeService 点赞          |
| `remove(key, value)`   | `SREM`      | 删成员         | LikeService 取消点赞        |
| `isMember(key, value)` | `SISMEMBER` | 判断成员在不在     | LikeService 判断已赞 / 布隆过滤 |
| `size(key)`            | `SCARD`     | 统计成员数       | LikeService 点赞数         |
| `intersect(k1, k2)`    | `SINTER`    | 交集          | LikeService 共同点赞        |

### ④ ZSet（`opsForZSet()`）—— 带「分数」的 Set，自动按分数排序

| Java 方法                             | Redis 命令               | 含义           | 出现处              |
|-------------------------------------|------------------------|--------------|------------------|
| `incrementScore(key, value, delta)` | `ZINCRBY`              | 分数原子累加       | RankService 加分   |
| `reverseRank(key, value)`           | `ZREVRANK`             | 从高到低的名次（0 起） | RankService 查排名  |
| `score(key, value)`                 | `ZSCORE`               | 查某成员分数       | RankService 查分   |
| `reverseRange(key, 0, n-1)`         | `ZREVRANGE`            | 取前 N 名（高到低）  | RankService TopN |
| `reverseRangeWithScores(...)`       | `ZREVRANGE WITHSCORES` | 前 N 名 + 分数   | RankService 榜单   |

### ⑤ Redisson 分布式锁（不是 Redis 命令，是封装）

| Java 方法                                   | 作用                                    |
|-------------------------------------------|---------------------------------------|
| `redissonClient.getLock(key)`             | 拿一把锁对象                                |
| `lock.tryLock(wait, leaseTime, TimeUnit)` | 尝试加锁（最多等 wait 秒，锁 leaseTime 秒，看门狗会续期） |
| `lock.isHeldByCurrentThread()`            | 判断是不是当前线程持有（幂等安全）                     |
| `lock.unlock()`                           | 释放锁（内部 Lua 校验持有者，防误删）                 |

### ⑥ Spring Cache 注解（本质是封装了上面的 RedisTemplate）

| 注解            | 等价逻辑                     |
|---------------|--------------------------|
| `@Cacheable`  | 查缓存 → 命中返回 → 未命中执行方法并写缓存 |
| `@CachePut`   | 执行方法后，把返回值写回缓存           |
| `@CacheEvict` | 执行方法后，删缓存                |

---

## 2. 一个「万能阅读模板」（看任何 Service 都套用）

拿到一个看不懂的 Service，只问 4 个问题：

1. **它 `opsForXxx()` 是哪一种？** → 决定底层是 String / Hash / Set / ZSet 的哪一种。
2. **它写/读/删了什么 key？** → 对照 `RedisKeyConstants`，key 名本身就是业务含义。
3. **TTL 设了多久？为什么？** → 过期时间 = 这个数据的「保鲜期」。
4. **类注释里「为什么用这个结构」？** → 这是本项目最值钱的部分（选型理由）。

例子：看 `UserCacheService`——
- `opsForValue()` → String；
- key 是 `user:info:{id}`，value 是 User 对象的 JSON；
- `set(key, user, 30分钟)` → 30 分钟后缓存失效重建；
- 注释说「整体读写对象用 String，频繁改字段才用 Hash」。

---

## 3. 建议学习顺序（先命令 → 再 Java → 再场景）

```
第 0 步：redis-cli 亲手敲一遍命令（1 小时，最关键）
  redis-cli
  SET k v / GET k / EXPIRE k 60 / TTL k
  HSET u name zhangsan / HGETALL u
  SADD s a b / SISMEMBER s a / SCARD s
  ZADD z 10 a / ZINCRBY z 5 a / ZREVRANGE z 0 -1 WITHSCORES
  ↑ 敲完这 10 条，再看 Java 代码会「自动翻译」

第 1 步：对照本表读 3 个配置 + 1 个常量（30 分钟）
  RedisConfig / RedissonConfig / AppProperties / RedisKeyConstants

第 2 步：按「模式」读案例（把 10 案例压成 6 类，别逐个啃）
  缓存类    ：案例1 String 缓存、案例2 Hash、案例8 穿透、SpringCache
  计数类    ：案例7 限流（INCR+EXPIRE）
  集合类    ：案例3 点赞（Set）、案例4 排行（ZSet）
  会话类    ：案例5 验证码、案例6 Token（TTL 过期/续期）
  并发类    ：案例9 秒杀（Redisson 分布式锁）
  消息类    ：案例10 Pub/Sub

第 3 步：跑测试 + 用 redis-cli 盯着 key 变化（1 小时）
  开一个终端 redis-cli MONITOR，另一个终端 mvn test，
  你会看到每行 Java 代码实际发出的 Redis 命令，理解直接拉满。

第 4 步：合上代码，用 1 分钟复述 7 个面试点（见下）
```

---

## 4. 1 分钟面试锚点（记忆口诀）

1. **为什么快**：内存 + 单线程 + IO 多路复用
2. **选型**：缓存 String/Hash、点赞 Set、榜单 ZSet
3. **三大问题**：穿透（空对象+布隆）、击穿（互斥锁）、雪崩（随机 TTL）
4. **一致性**：Cache Aside（先库后删）+ 延迟双删
5. **分布式锁**：Redisson 看门狗自动续期 + Lua 原子
6. **持久化**：RDB 快照快、AOF 日志稳
7. **高可用**：主从 → Sentinel → Cluster
