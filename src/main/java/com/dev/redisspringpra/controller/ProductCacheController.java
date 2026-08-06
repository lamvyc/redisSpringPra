package com.dev.redisspringpra.controller;

import com.dev.redisspringpra.common.Result;
import com.dev.redisspringpra.dto.ProductUpdateRequest;
import com.dev.redisspringpra.entity.Product;
import com.dev.redisspringpra.service.ProductCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 案例2：商品详情缓存 Controller（Hash）
 * <p>
 * 测试接口：
 * GET  /api/product/{id}       查询商品（Hash 缓存）
 * PUT  /api/product/{id}       更新商品（字段级更新缓存）
 * POST /api/product/{id}/tag   给商品加标签（Hash 动态扩展）
 * DELETE /api/product/{id}/cache 删除商品缓存
 */
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductCacheController {

    private final ProductCacheService productCacheService;

    /** 查询商品详情（Hash 缓存优先） */
    @GetMapping("/{id}")
    public Result<Product> getProduct(@PathVariable Long id) {
        return Result.success(productCacheService.getProductById(id));
    }

    /** 更新商品（只更新传入的字段，缓存同步更新对应字段） */
    @PutMapping("/{id}")
    public Result<Product> updateProduct(@PathVariable Long id, @RequestBody ProductUpdateRequest request) {
        return Result.success(productCacheService.updateProduct(id, request.getName(), request.getPrice(), request.getDescription()));
    }

    /** 给商品加标签（演示 Hash 动态增加字段） */
    @PostMapping("/{id}/tag")
    public Result<Void> addTag(@PathVariable Long id, @RequestParam String tag) {
        productCacheService.addProductTag(id, tag);
        return Result.success();
    }

    /** 删除商品缓存 */
    @DeleteMapping("/{id}/cache")
    public Result<Void> deleteCache(@PathVariable Long id) {
        productCacheService.deleteCache(id);
        return Result.success();
    }
}