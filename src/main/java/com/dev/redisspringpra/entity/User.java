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
import java.time.LocalDateTime;

/**
 * 用户实体（缓存数据载体）
 * <p>
 * 用户数据存储在 MySQL（JPA 映射 t_user 表），Redis 只做缓存副本。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID（主键自增） */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户名 */
    @Column(nullable = false, length = 50)
    private String name;

    /** 手机号 */
    @Column(length = 20)
    private String phone;

    /** 年龄 */
    private Integer age;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;
}