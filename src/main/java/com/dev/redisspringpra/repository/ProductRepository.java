package com.dev.redisspringpra.repository;

import com.dev.redisspringpra.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品表 Repository（对应原 MockDb 的商品表操作）
 * <p>
 * 提供：
 * - findById：根据主键查询（替代 MockDb.findProductById）
 * - save/update：保存或更新（替代 MockDb.updateProduct）
 * - deductStock：原子扣减库存，替代 MockDb.deductStock
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 原子扣减库存（对应 MockDb.deductStock）
     * <p>
     * 真实数据库使用 UPDATE ... WHERE stock > 0 保证并发安全：
     * - 数据库行锁保证两个并发请求不会同时把库存扣成负数；
     * - 返回受影响行数：> 0 表示扣减成功，= 0 表示库存不足。
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - 1 WHERE p.id = :productId AND p.stock > 0")
    int deductStock(@Param("productId") Long productId);
}