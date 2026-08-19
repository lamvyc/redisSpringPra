package com.dev.redisspringpra.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 作用：统一捕获 Controller 层抛出的异常，转换为规范化的 Result 返回，
 * 避免异常堆栈直接暴露给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 如果 Controller 抛出来的是 BizException，就交给这个方法处理。
    /** 处理业务异常（验证码错误、Token 过期等） */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    // 兜底处理其他异常;如果前面没有更具体的异常处理方法，那所有 Exception 都交给我。
    /** 处理其他未预期异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统繁忙，请稍后重试");
    }
}

/**
 * @RestControllerAdvice 负责“全局监听异常”
 * 正常情况下，Controller 里面如果抛异常：异常可能一路往外抛。而加了：@RestControllerAdvice之后，Spring 会知道：
 * “如果 Controller 层出现异常，去 GlobalExceptionHandler 里面找对应的处理方法。”
 *
 * com.dev.redisspringpra.common
 * ├── GlobalExceptionHandler.java
 * └── Result.java
 * 所以在 GlobalExceptionHandler 中可以直接写：
 * public Result<Void> handleBizException(...)
 * 而不需要：
 * import com.dev.redisspringpra.common.Result;
 *
 * 如果不在同一个 package
 * com.dev.redisspringpra.common
 * └── GlobalExceptionHandler.java
 *
 * com.dev.redisspringpra.response
 * └── Result.java
 * 那么就需要：
 * import com.dev.redisspringpra.response.Result;
 * 然后才能：
 * public Result<Void> handleBizException(...)
 *
 *
 * 为什么 Result.error() 能调用
 * Result 里面：
 * public static <T> Result<T> error(String message)
 * 这是一个 static 静态方法。
 * 所以可以直接：
 * Result.error("系统繁忙，请稍后重试");
 * 因为它属于类本身，不是某个 Result 对象。所以不用new
 *
 * */