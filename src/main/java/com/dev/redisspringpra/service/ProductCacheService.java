package com.dev.redisspringpra.service;

import com.dev.redisspringpra.common.BizException;
import com.dev.redisspringpra.constant.RedisKeyConstants;
import com.dev.redisspringpra.entity.Product;
import com.dev.redisspringpra.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 案例2：商品详情缓存（Hash 数据结构）
 * <p>
 * 为什么商品详情用 Hash 而不是 String？
 * - Hash 支持「字段级读写」：只更新价格时，不用整体序列化/反序列化对象；
 * - 商品字段可能很多（名称、价格、库存、描述...），Hash 天然映射字段结构；
 * - 实际企业场景：商品列表页通常只需要部分字段，Hash 可只取需要的字段。
 * <p>
 * 如果换 String 会有什么问题？
 * - 更新一个价格字段，需要先 GET 整个 JSON → 反序列化 → 改字段 → 再 SET 回去，
 *   在并发更新时容易发生覆盖（后写覆盖先写，丢更新）。
 * <p>
 * 为什么用 StringRedisTemplate？
 * - Hash 的 field/value 都是纯字符串，StringRedisTemplate 无需 JSON 序列化，
 *   redis-cli 查看时字段可读（HGETALL product:detail:1 直接可读）；
 * - 如果用 RedisTemplate，value 会被序列化成带引号的 JSON，字段不可读且易出错。
 * <p>
 * 存储结构：
 * Redis Key: product:detail:1
 * Fields:   id=1, name=iPhone 16 Pro, price=8999.00, stock=100, description=...
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductRepository productRepository;

    /** 缓存过期时间：1 小时 */
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /**
     * 查询商品详情
     * <p>
     * 流程：
     * 1. 判断 Hash 是否存在（hasKey）；
     * 2. 存在 → entries() 取出全部字段，组装成 Product（命中缓存）；
     * 3. 不存在 → 查数据库 → 用 putAll 一次性写入 Hash（缓存重建）。
     */
    public Product getProductById(Long productId) {
        String key = RedisKeyConstants.PRODUCT_DETAIL + productId;

        // 1. 检查 Hash 是否存在
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            // 2. Hash 命中：取出所有 field-value
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
            log.debug("【Hash缓存命中】key={}, fields={}", key, entries.keySet());
            return toProduct(entries);
        }
        log.debug("【Hash缓存未命中】key={}，查询数据库", key);

        // 3. 查数据库并缓存重建
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BizException("商品不存在"));

        // 4. 写入 Hash：field 是字段名，value 是字段值
        stringRedisTemplate.opsForHash().putAll(key, toMap(product));
        // 5. 设置过期时间
        stringRedisTemplate.expire(key, CACHE_TTL);
        log.debug("【Hash缓存重建】key={}, ttl={}小时", key, CACHE_TTL.toHours());
        return product;
    }

    /**
     * 更新商品（演示字段级更新）
     * <p>
     * 使用 HSET 只更新传入的字段，不整体重写 Hash。
     * 更新策略：先更新 DB，再更新缓存字段（这里缓存与 DB 同步更新，
     * 实际项目中也可用删除缓存 + 延迟双删保证一致性）。
     */
    public Product updateProduct(Long productId, String name, BigDecimal price, String description) {
        String key = RedisKeyConstants.PRODUCT_DETAIL + productId;

        // 1. 更新数据库
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BizException("商品不存在"));
        // 记录哪些字段变化了，用于「只更新变化的 Hash 字段」
        Map<String, String> changedFields = new HashMap<>();

        if (name != null) {
            product.setName(name);
            changedFields.put("name", name);
        }
        if (price != null) {
            product.setPrice(price);
            changedFields.put("price", price.toPlainString());
        }
        if (description != null) {
            product.setDescription(description);
            changedFields.put("description", description);
        }
        productRepository.save(product);
        log.debug("【更新数据库】productId={}, 变更字段={}", productId, changedFields.keySet());

        // 2. 同步更新缓存字段（HSET 只改变化的字段）
        // 为什么用 put 而不用 delete？字段级更新可避免「删除缓存后短暂击穿」
        if (!changedFields.isEmpty()) {
            stringRedisTemplate.opsForHash().putAll(key, changedFields);
            log.debug("【同步缓存字段】key={}, fields={}", key, changedFields.keySet());
        }
        return product;
    }

    /**
     * 演示 Hash 的动态字段扩展：给商品增加「标签」字段
     * <p>
     * Hash 的优势：新增字段不影响已有结构，无需迁移，直接 HSET 即可。
     */
    public void addProductTag(Long productId, String tag) {
        String key = RedisKeyConstants.PRODUCT_DETAIL + productId;
        // HSET product:detail:1 tags "旗舰,新品"
        stringRedisTemplate.opsForHash().put(key, "tags", tag);
        log.debug("【Hash扩展字段】key={}, field=tags, value={}", key, tag);
    }

    /** 删除缓存（用于演示缓存一致性） */
    public void deleteCache(Long productId) {
        String key = RedisKeyConstants.PRODUCT_DETAIL + productId;
        Boolean deleted = stringRedisTemplate.delete(key);
        log.debug("【删除缓存】key={}, 结果={}", key, deleted);
    }

    /** ========== 工具方法：Product <-> Hash Map ========== */

    /** Product 转 Hash Map（所有字段转为字符串存储） */
    private Map<String, String> toMap(Product product) {
        Map<String, String> map = new HashMap<>();
        map.put("id", String.valueOf(product.getId()));
        map.put("name", product.getName());
        map.put("price", product.getPrice().toPlainString());
        map.put("stock", String.valueOf(product.getStock()));
        map.put("description", product.getDescription());
        map.put("createTime", String.valueOf(product.getCreateTime()));
        return map;
    }

    /** Hash Map 转 Product */
    private Product toProduct(Map<Object, Object> entries) {
        return new Product(
                Long.valueOf((String) entries.get("id")),
                (String) entries.get("name"),
                new BigDecimal((String) entries.get("price")),
                Integer.valueOf((String) entries.get("stock")),
                (String) entries.get("description"),
                java.time.LocalDateTime.parse((String) entries.get("createTime"))
        );
    }
}