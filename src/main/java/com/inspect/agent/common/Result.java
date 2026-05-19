package com.inspect.agent.common;

import lombok.Data;

/**
 * 统一 API 响应体。
 *
 * @param <T> data 的类型
 */
@Data
public class Result<T> {

    /** 业务状态码，200 表示成功 */
    private int code;

    /** 提示消息 */
    private String message;

    /** 响应数据 */
    private T data;

    /** 响应时间戳（毫秒） */
    private long timestamp;

    private Result() {}

    /** 构建成功响应（含数据） */
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    /** 构建成功响应（自定义消息 + 数据） */
    public static <T> Result<T> ok(String message, T data) {
        Result<T> r = ok(data);
        r.message = message;
        return r;
    }

    /** 构建失败响应 */
    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.timestamp = System.currentTimeMillis();
        return r;
    }

    /** 构建失败响应（默认 code=500） */
    public static <T> Result<T> fail(String message) {
        return fail(500, message);
    }
}