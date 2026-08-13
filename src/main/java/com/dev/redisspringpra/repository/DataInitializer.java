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
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void run(String... args) {
        // 用户表为空才初始化
        Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_user", Long.class);
        if (userCount != null && userCount == 0) {
            log.info("【数据库初始化】插入 5 个演示用户");
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