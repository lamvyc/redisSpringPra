-- =============================================
-- Redis 学习实战项目：MySQL 初始化脚本
-- 数据库：redis_spring_pra
-- 说明：JPA ddl-auto=update 会自动建表，
--       本脚本只需创建数据库即可；
--       演示数据由 DataInitializer 首次启动自动插入。
-- =============================================

-- 1. 创建数据库（字符集 utf8mb4，支持中文 + emoji）
CREATE DATABASE IF NOT EXISTS redis_spring_pra
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE redis_spring_pra;

-- 2. 可选：手动建表 SQL（如果不想用 JPA 自动建表，可执行以下语句）
--    表名 t_user / t_product 与实体 @Table(name) 对应

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    name        VARCHAR(50)  NOT NULL COMMENT '用户名',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    age         INT          DEFAULT NULL COMMENT '年龄',
    create_time DATETIME(6)  DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- 商品表
CREATE TABLE IF NOT EXISTS t_product (
    id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    name        VARCHAR(100)  NOT NULL COMMENT '商品名称',
    price       DECIMAL(10,2) NOT NULL COMMENT '商品价格',
    stock       INT           NOT NULL COMMENT '库存',
    description VARCHAR(500)  DEFAULT NULL COMMENT '商品描述',
    create_time DATETIME(6)   DEFAULT NULL COMMENT '上架时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品表';