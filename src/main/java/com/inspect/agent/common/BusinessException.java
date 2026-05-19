package com.inspect.agent.common;

/**
 * 业务异常，用于中断流程并向前端返回明确的错误码和消息。
 * <p>默认 code=500，由 {@link GlobalExceptionHandler} 统一处理。
 */
public class BusinessException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(500, message);
    }

    public int getCode() {
        return code;
    }
}