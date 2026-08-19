package com.dev.redisspringpra.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类 —— 用于分布式锁
 * <p>
 * Redisson 是什么？
 * - 一个基于 Redis 的 Java 客户端框架，封装了分布式锁、分布式对象、分布式集合等高级功能。
 * - 相比手写 SETNX 分布式锁，Redisson 内置了「看门狗自动续期」「可重入」「公平锁」等能力。
 * <p>
 * 为什么用 Redisson 而不是手写 SETNX？
 * - 手写 SETNX 需要自己处理：锁过期时间、自动续期、释放锁时的原子校验，容易出 bug；
 * - Redisson 的 getLock() 基于 Lua 脚本实现原子性，看门狗线程默认每 10 秒续期一次，防止业务没执行完锁就过期。
 */
@Configuration
public class RedissonConfig {

    @Value("${redisson.single-server-config.address:redis://localhost:6379}")
    private String address;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 单机模式（生产环境可用 .useSentinelServers() / .useClusterServers()）
        config.useSingleServer()
                .setAddress(address);
        // 根据配置创建真正的 RedissonClient。
        return Redisson.create(config);
    }
}

/**
 * @Configuration告诉 Spring：这个类是一个“配置类”，里面会定义一些需要交给 Spring 管理的对象。
 *
 * @Value = 从配置文件中读取一个值，注入到 Java 属性里。
 * @Value("${redisson.single-server-config.address:redis://localhost:6379}")
 * private String address;
 * 放在一起看，@Value是修饰address的
 * 去配置文件找 redisson.single-server-config.address，如果找不到，就使用 redis://localhost:6379。
 * 找到了就使用配置文件里的
 * */