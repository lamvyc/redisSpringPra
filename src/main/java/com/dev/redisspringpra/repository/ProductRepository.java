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
 *
 * <p>@Repository => 这是数据访问层组件
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
    /**
     *  [参数注解] [参数类型] [参数名]
     *  @Param("productId") Long productId
     *     ↑                        ↑
     *  给 Spring 用             Java变量名
     *
     *  :productId 是 SQL/JPQL 里的参数占位符，@Param("productId") 告诉 Spring：这个占位符的值来自哪个方法参数。
     *  所以如果在外部调用，比如productRepository.deductStock(1001L);
     *  productId或者:productId相当于传值为 1001 的 long 类型数字
     *
    */
}

/**
 * @Query = 定义要执行的 JPQL
 * 以后有人调用 deductStock()，你就帮我执行@Query里面的这条 JPQL。
 * <p>
 * 注意：
 * JPQL -> 面向 Java Entity
 * SQL  -> 面向数据库表
 * 也就是说：
 * Product,p.stock,p.id对应的是Product实体，Product实体的stock属性，Product实体的id属性
 *
 * @Query     -> 我定义了一条自定义 JPQL
 * @Modifying -> 这条 JPQL 是 UPDATE / DELETE / INSERT 这种修改操作
 * @Modifying = 这是UPDATE/DELETE/INSERT等修改操作，不是查询
 *
 * @Transactional = 在事务中执行
 * 事务的核心思想就是：
 * 开始事务 -> 执行数据库操作
 *                ↓
 *            成功 → 提交
 *            失败 → 回滚
 * <p>
 * 对于上面代码可以粗略理解为：
 * 调用 deductStock() -> 开启事务 -> 执行 UPDATE -> 库存扣减成功 -> 提交事务
 * <p>
 *
 *
 * | 注解               | 你现在怎么记            |
 * | ---------------- | ----------------- |
 * | `@Query`         | **自己写查询/修改语句**    |
 * | `@Modifying`     | **告诉 JPA：这是修改操作** |
 * | `@Transactional` | **放在事务里执行**       |
 *
 * */


/**
 * 上面的都是：是什么，有什么用，下面的是
 * 使用场景/判断规则：
 * 【需求】
 * 需要自己定义一条 JPQL
 *     ↓
 * @Query
 *
 * 【需求】
 * @Query 执行的是 UPDATE / DELETE
 *     ↓
 * @Modifying
 *
 * 【需求】
 * 数据库操作需要事务保证
 *     ↓
 * @Transactional
 *
 * 【需求】
 * JPQL 中需要接收 Java 方法参数
 *     ↓
 * :参数名 + @Param("参数名")
 * <p>
 *
 * 【是什么】    → 概念
 * 【有什么用】  → 作用
 * 【怎么用】    → 用法
 * 【什么时候用】→ 使用场景 / 判断规则 ⭐
 * 【为什么这样用】→ 原理
 * */