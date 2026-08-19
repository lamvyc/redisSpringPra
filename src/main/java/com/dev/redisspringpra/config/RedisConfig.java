package com.dev.redisspringpra.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 配置类
 * <p>
 * 核心作用：
 * 1. 解决 RedisTemplate 默认 JDK 序列化导致的「乱码、不可读、体积大」问题；
 * 2. 统一 Key 使用 String 序列化（可读），Value 使用 JSON 序列化（跨语言）；
 * 3. 配置 Spring Cache 的序列化方式与过期时间。
 * <p>
 * 面试点：为什么 key 用 String、value 用 JSON？
 * - key 用 String：redis-cli 可读、便于排查问题；
 * - value 用 JSON：体积小、可跨语言（Java/Python/Go 都能读）。
 * <p>
 * 为什么自定义 ObjectMapper？
 * 1. 必须注册 JavaTimeModule：实体中的 LocalDateTime 才能序列化；
 * 2. 必须开启默认类型写入（activateDefaultTyping）：
 *    Redis 不知道 Java 对象类型，写入 @class 字段后反序列化才能还原成 User/Product。
 */
@Configuration // 这是一个 Spring 配置类
@EnableCaching // 开启 Spring Cache 功能。
public class RedisConfig {

    /**
     * 构建带类型信息的 ObjectMapper
     * <p>
     * 流程：JavaTimeModule（时间支持） + DefaultTyping（类型信息） + 全字段可见（getter 都能序列化）
     */
    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 支持 LocalDateTime 等 Java 8 时间类型
        mapper.registerModule(new JavaTimeModule());
        // 序列化时记录类型信息（反序列化时需要还原对象类型）
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        // 所有字段可见（包括 private，无需 getter 也能序列化）
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        return mapper;
    }

    /**
     * 自定义 RedisTemplate
     * <p>
     * 为什么不直接用 Spring Boot 自动配置的 RedisTemplate？
     * - 默认 RedisTemplate 使用 JdkSerializationRedisSerializer，
     *   存入 Redis 的 key 会变成 "\xac\xed..." 乱码，且不支持跨语言。
     * - 这里重新指定 key/value 的序列化器。
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Key 使用 String 序列化 —— 保证 key 可读、方便 redis-cli 查询
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 JSON 序列化（含类型信息 + 时间支持）—— 体积小、可跨语言、可还原对象
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(buildObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Spring Cache 缓存管理器
     * <p>
     * 配置作用：
     * 1. 缓存 key 使用 String 序列化（可读）；
     * 2. 缓存 value 使用 JSON 序列化；
     * 3. 全局默认过期时间 10 分钟，防止缓存数据无限堆积。
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // key 使用 String 序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // value 使用 JSON 序列化（含类型信息 + 时间支持）
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(buildObjectMapper())))
                // 默认过期时间 10 分钟
                // 注意：Spring Data Redis 3.x 默认允许缓存 null 值，配合「空对象」防止缓存穿透
                .entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}

/**
 * 最应该记的其实只有 8 个东西
 * ① @Configuration
 *    ↓
 *    这是一个 Spring 配置类
 *
 * ② @Bean
 *    ↓
 *    把对象交给 Spring 管理
 *
 * ③ RedisConnectionFactory
 *    ↓
 *    Redis 连接工厂
 *
 * ④ RedisTemplate
 *    ↓
 *    Java 手动操作 Redis
 *
 * ⑤ StringRedisSerializer
 *    ↓
 *    Key → String
 *
 * ⑥ GenericJackson2JsonRedisSerializer
 *    ↓
 *    Value → JSON
 *
 * ⑦ @EnableCaching
 *    ↓
 *    开启 Spring Cache
 *
 * ⑧ CacheManager
 *    ↓
 *    管理 @Cacheable 等缓存
 *
 * 然后再补
 * ObjectMapper
 *     ↓
 * Java ↔ JSON
 *
 * JavaTimeModule
 *     ↓
 * 支持 LocalDateTime
 *
 * DefaultTyping
 *     ↓
 * 保存 Java 类型信息
 *
 * TTL
 *     ↓
 * 缓存自动过期
 *
 *
 *
 *
 * 这整个类其实就干 3 件事：
 * RedisConfig
 * │
 * ├── ① 告诉 RedisTemplate：
 * │      Key 怎么存？
 * │      Value 怎么存？
 * │
 * ├── ② 告诉 Spring Cache：
 * │      Key 怎么存？
 * │      Value 怎么存？
 * │      默认过期多久？
 * │
 * └── ③ 告诉 Jackson：
 *        Java 对象怎么变成 JSON？
 *        JSON 怎么变回 Java 对象？
 *        LocalDateTime 怎么处理？
 *
 * 实际上是：
 * RedisConfig
 *     │
 *     ├── ObjectMapper
 *     │      ↓
 *     │   负责 JSON 序列化规则
 *     │
 *     ├── RedisTemplate
 *     │      ↓
 *     │   负责你手动操作 Redis 时的数据格式
 *     │
 *     └── CacheManager
 *            ↓
 *         负责 @Cacheable 等 Spring Cache
 *
 * */