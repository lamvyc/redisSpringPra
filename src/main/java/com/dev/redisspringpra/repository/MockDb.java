package com.dev.redisspringpra.repository;

import com.dev.redisspringpra.entity.Product;
import com.dev.redisspringpra.entity.User;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟数据库（用内存 Map 代替 MySQL，聚焦 Redis 学习）
 * <p>
 * 实际项目中：
 * - 数据存在 MySQL，通过 MyBatis/JPA 查询；
 * - 这里的 Map 相当于「数据库表」，提供查询、更新能力。
 *
 * <p>
 * 为什么用 ConcurrentHashMap？
 * - 模拟数据库需要线程安全；
 * - 实际企业中数据库天然具备事务、索引、并发控制能力，这里从简。
 */
@Component
public class MockDb {

    /** 模拟用户表 */
    private final Map<Long, User> userTable = new ConcurrentHashMap<>();

    /** 模拟商品表 */
    private final Map<Long, Product> productTable = new ConcurrentHashMap<>();

    /** 初始化模拟数据 */
    @PostConstruct
    public void init() {
        LocalDateTime now = LocalDateTime.now();

        // 预置 5 个用户
        for (long i = 1; i <= 5; i++) {
            userTable.put(i, new User(i, "用户" + i, "1380000000" + i, 20 + (int) i, now));
        }

        // 预置 3 个商品
        productTable.put(1L, new Product(1L, "iPhone 16 Pro", new BigDecimal("8999.00"), 100, "苹果旗舰手机", now));
        productTable.put(2L, new Product(2L, "MacBook Pro", new BigDecimal("16999.00"), 50, "苹果笔记本", now));
        productTable.put(3L, new Product(3L, "AirPods Pro", new BigDecimal("1899.00"), 200, "苹果降噪耳机", now));
    }

    /** ========== 用户表操作 ========== */

    /** 根据 ID 查询用户（模拟 MySQL SELECT） */
    public Optional<User> findUserById(Long userId) {
        return Optional.ofNullable(userTable.get(userId));
    }

    /** 更新用户（模拟 MySQL UPDATE） */
    public void updateUser(User user) {
        userTable.put(user.getId(), user);
    }

    /** ========== 商品表操作 ========== */

    /** 根据 ID 查询商品（模拟 MySQL SELECT） */
    public Optional<Product> findProductById(Long productId) {
        return Optional.ofNullable(productTable.get(productId));
    }

    /** 更新商品（模拟 MySQL UPDATE） */
    public void updateProduct(Product product) {
        productTable.put(product.getId(), product);
    }

    /** 扣减库存（模拟 MySQL UPDATE stock = stock - 1 WHERE stock > 0） */
    public boolean deductStock(Long productId) {
        Product product = productTable.get(productId);
        if (product != null && product.getStock() > 0) {
            product.setStock(product.getStock() - 1);
            return true;
        }
        return false;
    }
}