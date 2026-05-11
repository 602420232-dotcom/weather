package com.uav.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 服务不可用异常
 * 用于标识下游服务调用失败（被熔断器保护）
 */
public class ServiceUnavailableException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private final String serviceName;
    private final HttpStatus httpStatus;

    public ServiceUnavailableException(String serviceName, String message) {
        super(message);
        this.serviceName = serviceName;
        this.httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
    }

    public ServiceUnavailableException(String serviceName, String message, HttpStatus httpStatus) {
        super(message);
        this.serviceName = serviceName;
        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.SERVICE_UNAVAILABLE;
    }

    public ServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
    }

    public String getServiceName() {
        return serviceName;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public static ServiceUnavailableException serviceDown(String serviceName, String message) {
        return new ServiceUnavailableException(serviceName, message, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static ServiceUnavailableException gatewayTimeout(String serviceName, String message) {
        return new ServiceUnavailableException(serviceName, message, HttpStatus.GATEWAY_TIMEOUT);
    }

    public static ServiceUnavailableException badGateway(String serviceName, String message) {
        return new ServiceUnavailableException(serviceName, message, HttpStatus.BAD_GATEWAY);
    }

    public static ServiceUnavailableException circuitBreakerOpen(String serviceName) {
        return new ServiceUnavailableException(
                serviceName,
                "服务 " + serviceName + " 熔断器已打开，暂时不可用",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }
}
