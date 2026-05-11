package com.uav.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 业务异常
 * 支持自定义HTTP状态码，便于区分不同类型的业务错误
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private final String code;
    private final HttpStatus httpStatus;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String code, String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.code = code;
        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.BAD_REQUEST;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static BusinessException badRequest(String code, String message) {
        return new BusinessException(code, message, HttpStatus.BAD_REQUEST);
    }

    public static BusinessException unauthorized(String code, String message) {
        return new BusinessException(code, message, HttpStatus.UNAUTHORIZED);
    }

    public static BusinessException forbidden(String code, String message) {
        return new BusinessException(code, message, HttpStatus.FORBIDDEN);
    }

    public static BusinessException notFound(String code, String message) {
        return new BusinessException(code, message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(code, message, HttpStatus.CONFLICT);
    }

    public static BusinessException unprocessableEntity(String code, String message) {
        return new BusinessException(code, message, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static BusinessException tooManyRequests(String code, String message) {
        return new BusinessException(code, message, HttpStatus.TOO_MANY_REQUESTS);
    }

    public static BusinessException internal(String code, String message) {
        return new BusinessException(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
