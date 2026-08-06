package com.dev.redisspringpra.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体（缓存数据载体）
 * <p>
 * 实际项目中用户数据存在 MySQL，Redis 只做缓存副本；
 * 这里用内存 Map 模拟 DB，聚焦 Redis 学习。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    private Long id;

    /** 用户名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 年龄 */
    private Integer age;

    /** 创建时间 */
    private LocalDateTime createTime;
}