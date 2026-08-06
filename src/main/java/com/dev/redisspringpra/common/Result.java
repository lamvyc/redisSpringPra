package com.dev.redisspringpra.common;

import lombok.Data;

/**
 * 统一 API 返回结果
 * <p>
 * 为什么需要统一返回结构？
 * - 前后端约定：code 表示业务状态码，message 表示提示信息，data 表示业务数据；
 * - 避免每个接口返回格式不一致，后期难以维护。
 */
@Data
public class Result<T> {

    /** 业务状态码：200 成功，其他为失败 */
    private Integer code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}