package com.dev.redisspringpra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RedisSpringPraApplication {

    /**
     * <p>@SpringBootApplication</p>
     *         │
     *         ├── @SpringBootConfiguration
     *         │       └── 表明这是 Spring Boot 配置类
     *         │
     *         ├── @EnableAutoConfiguration
     *         │       └── 开启 Spring Boot 自动配置
     *         │
     *         └── @ComponentScan
     *                 └── 扫描当前包及子包中的 Spring 组件
     *
     *
     * RedisSpringPraApplication.class
     * Spring Boot 需要知道：
     * 从哪个类开始启动、从哪个包开始进行组件扫描。
     *
     * args是 Java 程序启动时传入的命令行参数
     * 比如java -jar app.jar --server.port=8081
     * --server.port=8081是启动参数
     *
     * */

    // ① Java 程序的入口
    public static void main(String[] args) {
        // ② Spring Boot 应用的启动入口
        // ③ Spring 容器的创建/启动入口
        SpringApplication.run(RedisSpringPraApplication.class, args);
    }
}
