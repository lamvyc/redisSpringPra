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

    private final Integer code;

    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}