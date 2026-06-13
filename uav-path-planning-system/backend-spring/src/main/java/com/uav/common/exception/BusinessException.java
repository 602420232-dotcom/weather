package com.uav.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public static BusinessException forbidden(String code, String message) {
        return new BusinessException(code, message);
    }

    public static BusinessException badRequest(String code, String message) {
        return new BusinessException(code, message);
    }

    public static BusinessException notFound(String code, String message) {
        return new BusinessException(code, message);
    }
}
