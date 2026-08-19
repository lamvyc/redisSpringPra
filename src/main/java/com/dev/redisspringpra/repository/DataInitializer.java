package com.dev.redisspringpra.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据库初始化：首次启动时预置演示数据（替代原 MockDb 的 @PostConstruct）
 * <p>
 * 为什么用 JdbcTemplate 原生 SQL 而不是 JpaRepository.save()？
 * - 演示数据需要「显式指定 id=1~5」（多个案例依赖固定 ID，如缓存 Key user:1）；
 * - JPA 的 save() 对已设置 id 的实体走 merge（UPDATE 语义），表里无此行会抛
 *   StaleObjectStateException；
 * - 原生 INSERT 语义明确，也是真实项目「种子数据/数据导入」的常见做法。
 */
@Slf4j
@Component  // 交给 Spring 管理
@RequiredArgsConstructor  // 自动生成构造器注入依赖
public class DataInitializer implements CommandLineRunner {
    // `@RequiredArgsConstructor` 只对 **`final`** 或 **`@NonNull`** 字段生成构造器。其核心作用还是依赖注入
    // @RequiredArgsConstructor帮我生成构造器，把 JdbcTemplate 注入进来
    // JdbcTemplate 是 Spring 提供的一个数据库操作工具类，可以让你方便执行 SQL
    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run(String... args) { // 数据库初始化
        // 用户表为空才初始化
        Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user", Long.class);
        if (userCount != null && userCount == 0) {
            log.info("【数据库初始化】插入 5 个演示用户");
            // LocalDateTime now = LocalDateTime.now();得到的是一个 LocalDateTime：2026-08-19T21:50:30.123
            LocalDateTime now = LocalDateTime.now();
            for (long i = 1; i <= 5; i++) {
                jdbcTemplate.update(
                        "INSERT INTO t_user (id, name, phone, age, create_time) VALUES (?, ?, ?, ?, ?)",
                        i, "用户" + i, "1380000000" + i, 20 + (int) i, now.format(DT)
                );
            }
        }

        // 商品表为空才初始化
        Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_product", Long.class);
        if (productCount != null && productCount == 0) {
            log.info("【数据库初始化】插入 3 个演示商品");
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(
                    "INSERT INTO t_product (id, name, price, stock, description, create_time) VALUES (?, ?, ?, ?, ?, ?)",
                    1L, "iPhone 16 Pro", new BigDecimal("8999.00"), 100, "苹果旗舰手机", now.format(DT)
            );
            jdbcTemplate.update(
                    "INSERT INTO t_product (id, name, price, stock, description, create_time) VALUES (?, ?, ?, ?, ?, ?)",
                    2L, "MacBook Pro", new BigDecimal("16999.00"), 50, "苹果笔记本", now.format(DT)
            );
            jdbcTemplate.update(
                    "INSERT INTO t_product (id, name, price, stock, description, create_time) VALUES (?, ?, ?, ?, ?, ?)",
                    3L, "AirPods Pro", new BigDecimal("1899.00"), 200, "苹果降噪耳机", now.format(DT)
            );
        }
    }
}

/**
 * 为什么使用@Component(作用：交给 Spring 管理)
 * @Component 是最通用的 Spring Bean 注册注解；
 * @Service、@Repository、@Controller 则是在 @Component 基础上表达更具体的职责。
 * <p>
 *
 * CommandLineRunner 是 Spring Boot 提供的接口。
 * Spring Boot 启动完成后，自动执行 run() 方法。
 * ① 启动时加载内存配置
 *    ↓
 *    数据库 → 内存 Map
 *
 * ② 启动时预热缓存
 *    ↓
 *    数据库 → Redis
 *
 * ③ 初始化一些运行环境
 *    ↓
 *    创建目录、检查文件等
 *
 * ④ 启动检查
 *    ↓
 *    检查某些依赖是否正常
 *
 * ⑤ 初始化少量系统基础数据
 *    ↓
 *    创建默认配置 / 默认账号等
 *
 *
 * @Slf4j = 自动给当前类提供 log 日志对象。
 * 它是 Lombok 注解，会在编译时帮你生成类似：
 * private static final Logger log =
 *         LoggerFactory.getLogger(DataInitializer.class);
 *
 * jdbcTemplate：负责操作数据库。
 * JdbcTemplate
 * │
 * ├── queryForObject()
 * │   → 查询一条结果，并转换成指定 Java 类型
 * │
 * ├── query()
 * │   → 查询多条结果
 * │
 * └── update()
 *     → INSERT / UPDATE / DELETE
 *
 * DT：负责格式化时间。
 * now.format(DT)会按照这里定义的格式：yyyy-MM-dd HH:mm:ss
 * 转换成：
 * 2026-08-19 21:50:30
 *
 * */