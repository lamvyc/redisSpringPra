package com.dev.redisspringpra.common;

import lombok.Getter;

/**
 * 业务异常
 * <p>
 * 为什么需要自定义异常？
 * - 用 code 区分业务错误类型（如验证码错误、Token 过期、限流），
 *   前端可以根据 code 做不同的交互提示。
 */
@Getter
public class BizException extends RuntimeException {
    // RuntimeException：让这个类成为“运行时异常”，业务代码可以直接 throw，不用层层 throws/catch。

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * throw new BizException(1001, "验证码错误");
     * 得到
     * code    = 1001
     * message = 验证码错误
     * */
}

/**
 * @Getter会自动帮你生成 getter 方法。
 * 相当于你手写：
 * public Integer getCode() {
 *     return code;
 * }
 *
 * 受检异常：Java 编译器强制你处理。
 * 非受检异常：Java 编译器不强制你处理。
 *
 * 受检 / 非受检的区别，主要发生在编译阶段：Java 是否强制开发者声明或处理异常。
 * 到了程序运行阶段，异常该怎么处理、会不会影响请求或服务，则取决于实际的异常处理逻辑
 *
 * */