# 案例6 · 登录 Token（String + TTL + 滑动续期）

> 技术方案：String 存 token→userId 映射，TTL 30 分钟，每次校验续期
> 学什么：**滑动过期（续期）** —— 与案例5 固定过期的重要区别
>
> 脚手架见《00-总纲》，本文只讲增量。

---

# Part A · 看懂版

## A1. 全景流程图

```
POST /api/token/login?userId=1
   │
   ▼
TokenService.login(1)
   └─ 生成 UUID → set("user:token:"+token, userId, 30min)   = SET user:token:xxx 1 EX 1800

GET /api/token/verify?token=xxx
   │
   ▼
TokenService.verifyToken(token)
   ├─ ① get(key) 拿 userId         = GET user:token:xxx
   │       （null → 抛"Token 无效或已过期"）
   ├─ ② expire(key, 30min) 续期    = EXPIRE user:token:xxx 1800   ← 核心：滑动过期
   └─ ③ 查库返回用户信息
```

## A2. 核心：Token 为什么用 String？为什么不用 MySQL？

- Token = 「一个随机串 → 一个 userId」的键值映射，String 最自然。
- **每次请求都要校验**，高频读，放 MySQL 成热点瓶颈；Redis 自带过期，天然适合会话管理。

## A3. 逐行翻译 + 面试点

```java
// login —— 登录
String token = UUID.randomUUID().toString().replace("-", "");   // 生成唯一 token
stringRedisTemplate.opsForValue().set(key, String.valueOf(userId), expire);  // SET ... EX 1800

// verifyToken —— 校验（核心在"续期"）
Object userIdObj = stringRedisTemplate.opsForValue().get(key);   // GET
if (userIdObj == null) throw new BizException(401, "Token 无效或已过期");

stringRedisTemplate.expire(key, expire);   // EXPIRE：续期！每次校验重置 30 分钟
```

### ⭐ 什么是「滑动过期」？（面试必问）

| 方案 | 行为 | 效果 |
|------|------|------|
| 固定过期（案例5 验证码） | 写入后 5 分钟必过期，不续 | 到期必须重新获取 |
| 滑动过期（案例6 Token） | 每次访问都重置为 30 分钟 | **持续活跃永不过期，不活跃才过期** |

这就是「退出登录主动删 token」之外，用户"长时间不操作自动掉线"的实现原理。

```java
// logout —— 退出
stringRedisTemplate.delete(key);   // DEL：主动失效，防止 token 在过期前被继续用
```

## A4. 测试剧本

```java
token = login(1)                 // 登录拿 token
verifyToken(token)               // 校验通过（顺带续期）
verifyToken("invalid-token")     // 抛异常
logout(token)                    // 退出删 token
verifyToken(token)               // 再校验 → 抛异常（已删除）
```

## A5. 记忆锚点

- `expire(key, Duration)` = `EXPIRE`：**重置**过期时间（续期）。
- **固定过期**（验证码）vs **滑动过期**（Token）：区别就在「每次访问是否 reset TTL」。

---

# Part B · 从0实现版

## B1. 需求

> 登录发 token（30 分钟过期）；每次校验续期（滑动过期）；退出删除 token。

## B2. 内心独白

> token 本质是「随机串 → userId」的映射，用 String。要"持续活跃不掉线"，就得在每次校验时把 TTL 重置 —— 用 expire 续期。

## B3. 手写代码

```java
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redis;
    private final UserMapper userMapper;
    private static final Duration TTL = Duration.ofMinutes(30);

    public String login(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set("user:token:" + token, String.valueOf(userId), TTL); // SET EX
        return token;
    }

    public User verify(String token) {
        String key = "user:token:" + token;
        String userId = redis.opsForValue().get(key);        // GET
        if (userId == null) throw new RuntimeException("Token 无效或已过期");
        redis.expire(key, TTL);                               // EXPIRE 续期（滑动过期）
        return userMapper.selectById(Long.valueOf(userId));
    }

    public void logout(String token) {
        redis.delete("user:token:" + token);                  // DEL 主动失效
    }
}
```

## B4. 与案例5 的区别（记住这一句）

> 验证码是**固定过期**（到期必失效、一次性删除）；Token 是**滑动过期**（每次校验续期，活跃不掉线）。区别就在「校验成功后有没有那句 `expire`」。

## B5. 自测标准

> 能说出「滑动过期」是什么、和「固定过期」的区别，并能默写出 `login`/`verify`/`logout` 三个方法（重点是 verify 里的 `expire` 续期），案例6 就掌握了。
