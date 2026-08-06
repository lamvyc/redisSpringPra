package com.dev.redisspringpra.entity;

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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    private Long id;

    /** 商品名称 */
    private String name;

    /** 商品价格 */
    private BigDecimal price;

    /** 库存 */
    private Integer stock;

    /** 商品描述 */
    private String description;

    /** 上架时间 */
    private LocalDateTime createTime;
}