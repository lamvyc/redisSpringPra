我的学习目标：

采用「索引式学习」方式学习 Redis。

当前已完成：
Java、Java集合、Spring Boot、MySQL。

目标：
1. 建立 Redis 知识体系；
2. 掌握 Spring Boot + Redis 实战；
3. 能阅读、修改项目 Redis 代码；
4. 能审查 AI 生成代码；
5. 达到 Java 后端面试表达水平。

学习原则：

不要输出百科。
优先：
业务场景 > 代码实践 > 核心原理 > 源码细节。

源码、底层数据结构只在需要时展开。


==============================
项目要求
==============================

整个学习过程只维护一个 Spring Boot 项目：

redis-learning

所有案例持续迭代，不创建多个 Demo。

要求：
- 复用已有 Entity、Service、Config、Utils；
- 保持统一代码结构；
- 新案例只新增相关代码；
- 最终形成接近真实企业项目的 Redis 实战工程。


==============================
知识点输出格式
==============================

每个知识点按照：

## 1. 一句话核心版
说明：
- 是什么
- 解决什么问题
- 使用场景

## 2. 核心原理
使用流程：

请求
↓
Redis
↓
命中/未命中
↓
业务处理

说明：
- 为什么这样设计；
- 优缺点；
- 适用场景。

## 3. Java后端视角
说明：
- 项目哪里使用；
- Redis Key 如何设计；
- 为什么不用 MySQL。

## 4. Spring Boot案例
包含：
- Maven依赖
- yml配置
- RedisConfig
- Entity
- Service
- Controller
- 测试代码

要求：
- Spring Boot + RedisTemplate；
- 完整可运行；
- 关键代码中文注释；
- 解释 Redis API 使用原因。

## 5. 执行结果
展示：
请求 → Redis操作 → 返回结果。

## 6. 注意事项
总结：
- 常见坑；
- 面试重点。

## 7. 面试回答
输出：
面试官问题：
回答：

控制1分钟以内。


==============================
第一阶段 Redis基础
==============================

学习：

1. Redis是什么
2. NoSQL分类
3. Redis与MySQL区别
4. Redis为什么快
    - 内存
    - 单线程
    - IO多路复用
    - Redis6多线程
5. Redis安装（Mac）
    - Homebrew
    - redis-server
    - redis-cli
    - redis.conf
6. 常用命令


==============================
第二阶段 数据结构
==============================

每种类型包含：
特点 + API + Java操作 + 业务案例。


String：

API：
SET GET INCR DECR SETNX MSET

案例：
- 用户信息缓存
- Token
- 验证码
- 计数器


Hash：

API：
HSET HGET HGETALL HINCRBY

案例：
- 商品详情缓存
- 用户对象缓存


List：

API：
LPUSH RPUSH LPOP RPOP

案例：
- 消息队列
- 最新列表


Set：

API：
SADD SISMEMBER SCARD

案例：
- 点赞
- 标签
- 共同好友


ZSet：

API：
ZADD ZRANGE ZSCORE ZINCRBY

案例：
- 排行榜
- 积分排名


==============================
第三阶段 Spring Boot整合
==============================

学习：

1. RedisTemplate
2. StringRedisTemplate
3. 序列化
4. RedisConfig
5. Spring Cache

注解：

@Cacheable
@CachePut
@CacheEvict


==============================
第四阶段 Redis业务案例
==============================

所有案例在同一个项目完成。


案例1：用户信息缓存

技术：
String

实现：
查询、修改、删除缓存、缓存重建。


案例2：商品详情缓存

技术：
Hash

实现：
商品查询、更新、缓存一致性。


案例3：点赞统计

技术：
Set

实现：
点赞、取消点赞、判断点赞、统计数量。


案例4：排行榜

技术：
ZSet

实现：
积分排行、Top10、实时更新。


案例5：验证码

技术：
String + TTL

实现：
生成、校验、自动过期。


案例6：登录Token

技术：
String + TTL

实现：
登录、验证、退出、续期。


案例7：接口限流

技术：
INCR + EXPIRE

实现：
访问次数限制。


案例8：缓存穿透防护

技术：
缓存空对象、布隆过滤器思想。


案例9：分布式锁

技术：
Redisson

场景：
秒杀库存扣减、订单创建。

重点：
获取锁、自动续期、释放锁。


案例10：消息通知

技术：
Pub/Sub

实现：
发布、订阅、系统通知。


==============================
第五阶段 Redis面试专题
==============================

必须掌握：

缓存：

- 缓存穿透
- 缓存击穿
- 缓存雪崩
- 双写一致性
- 延迟双删
- Cache Aside


性能：

- BigKey
- HotKey
- Pipeline


持久化：

- RDB
- AOF
- Redis7混合持久化


高可用：

- 主从复制
- Sentinel
- Cluster


事务：

- MULTI
- EXEC
- DISCARD
- WATCH

重点：
Redis事务为什么不能回滚。


==============================
第六阶段 高级特性
==============================

学习：

Bitmap：
签到、状态标记

HyperLogLog：
UV统计

GEO：
附近的人

Stream：
消息队列

ACL：
权限控制


==============================
代码要求
==============================

所有代码：

- Java
- Spring Boot
- RedisTemplate
- 企业项目风格
- 中文注释
- 可运行


每个案例补充：

1. 为什么选择该数据结构；
2. 如果换其他结构有什么问题；
3. 实际项目如何优化。


最终目标：

完成后能够：

- 使用 Redis；
- 设计 Redis Key；
- 选择数据结构；
- 解决缓存问题；
- 阅读企业代码；
- 面试回答 Redis问题；
- 审查 AI 生成代码。