package com.dev.redisspringpra.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（Hash 缓存案例数据载体）
 * <p>
 * 商品详情适合用 Hash 存储：字段可单独读写，如只更新价格不用重写整个对象。
 * 商品数据存储在 MySQL（JPA 映射 t_product 表）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品 ID（主键自增） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 商品名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 商品价格 */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** 库存 */
    @Column(nullable = false)
    private Integer stock;

    /** 商品描述 */
    @Column(length = 500)
    private String description;

    /** 上架时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;
}