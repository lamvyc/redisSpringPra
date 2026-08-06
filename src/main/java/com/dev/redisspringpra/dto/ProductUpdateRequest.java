package com.dev.redisspringpra.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品更新请求体
 * <p>
 * 仅更新时需要的字段，价格单独处理以演示 Hash 的字段级操作。
 */
@Data
public class ProductUpdateRequest {

    /** 商品名称 */
    private String name;

    /** 商品价格 */
    private BigDecimal price;

    /** 商品描述 */
    private String description;
}