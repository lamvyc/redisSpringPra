# 案例11 · Spring Cache 注解（@Cacheable / @CachePut / @CacheEvict）

> 技术方案：用注解替代手写 RedisTemplate
> 学什么：**注解缓存 vs 手写缓存的取舍**（会用即可）
>
> 这是案例1 的「注解版」，本质一样，只是写法更省事。

---

# Part A · 看懂版

## A1. 核心：注解和手写是一回事

Spring Cache 本质还是**封装了 RedisTemplate**（通过 CacheManager 管理）。三个注解把案例1 手写的代码"藏"起来了：

| 注解 | 作用 | 等价手写逻辑（案例1） |
|------|------|---------------------|
| `@Cacheable` | 查询：先查缓存，未命中执行方法并写缓存 | `get` → 命中返回 / 未命中查库再 `set` |
| `@CachePut` | 更新：执行方法后把返回值写回缓存 | 改库 → `set` 更新缓存 |
| `@CacheEvict` | 删除：执行方法后删缓存 | 改库 → `delete` 删缓存 |

## A2. 逐行翻译

```java
@Cacheable(cacheNames = "user", key = "#userId")
public User getUserWithCache(Long userId) {
    return userRepository.findById(userId).orElseThrow(...);
}
// 执行流程：先查缓存 key=user::1 → 命中直接返回（方法体不执行）→ 未命中执行方法查库，返回值自动写缓存
// 关键：命中缓存时，方法体根本不会执行（所以方法里的 log 只在"未命中"时打印）
```

```java
@CachePut(cacheNames = "user", key = "#userId")
public User updateUserWithCache(Long userId, ...) {
    // 改库
    return user;
}
// 执行流程：先执行方法（改库），再用返回值刷新缓存
```

```java
@CacheEvict(cacheNames = "user", key = "#userId")
public void evictUserCache(Long userId) {
    // 执行后删缓存
}
```

## A3. 注解 vs 手写（面试点）

| | 注解 | 手写 RedisTemplate |
|---|------|-------------------|
| 优点 | 开发快、代码少 | 灵活可控 |
| 适用 | 缓存逻辑简单统一（详情页） | 复杂场景（分布式锁+缓存重建、延迟双删） |
| 案例 | 案例11 | 案例1-10 |

## A4. 测试剧本

```java
getUserWithCache(1)           // @Cacheable：首次查库 + 写缓存
updateUserWithCache(1, ...)   // @CachePut：改库 + 刷新缓存
getUserWithCache(1)           // @Cacheable：命中缓存，拿到最新数据
evictUserCache(1)             // @CacheEvict：删缓存
```

## A5. 记忆锚点

- `@Cacheable` 查（先缓存后方法）、`@CachePut` 改（先方法后写缓存）、`@CacheEvict` 删。
- 注解本质 = 封装 RedisTemplate；简单场景用注解，复杂场景手写。

---

# Part B · 从0实现版

## B1. 需求

> 用户查询/更新/删缓存，用注解实现（代码更少）。

## B2. 内心独白

> 案例1 的「查缓存→查库→写缓存」是样板代码，每个方法都要写一遍。Spring Cache 用注解把这套样板收敛了，我只写业务方法 + 一个注解。

## B3. 手写代码

```java
@Service
@RequiredArgsConstructor
public class SpringCacheService {

    private final UserMapper userMapper;

    @Cacheable(cacheNames = "user", key = "#userId")
    public User getUser(Long userId) {
        return userMapper.selectById(userId);   // 只有未命中时才执行这里
    }

    @CachePut(cacheNames = "user", key = "#userId")
    public User update(Long userId, String name) {
        User u = userMapper.selectById(userId);
        u.setName(name);
        userMapper.updateById(u);
        return u;                               // 返回值写回缓存
    }

    @CacheEvict(cacheNames = "user", key = "#userId")
    public void evict(Long userId) {
        // 执行后自动删缓存
    }
}
```

## B4. 与案例1 的区别（记住这一句）

> 案例1 手写 `get`/`set`/`delete`，案例11 用三个注解替代。**逻辑完全一样，只是写法不同。**

## B5. 自测标准

> 能说出三个注解各自对应案例1 的哪段手写逻辑，并知道「注解适合简单场景、手写适合复杂场景」，案例11 就掌握了。
