package com.dev.redisspringpra.dto;

import lombok.Data;

/**
 * 用户信息更新请求体
 * <p>
 * DTO 与 Entity 分离：避免数据库实体直接暴露给前端接口。
 */
@Data
public class UserUpdateRequest {

    /** 用户名 */
    private String name;

    /** 年龄 */
    private Integer age;
}