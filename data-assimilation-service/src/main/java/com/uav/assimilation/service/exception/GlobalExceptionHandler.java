package com.uav.assimilation.service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * 数据同化服务异常处理器
 *
 * 继承 common-utils 通用异常处理器，获得完整的异常处理能力
 * （NoHandlerFoundException、AccessDeniedException、PythonExecutionException 等），
 * 同时保留本服务的响应格式兼容性。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends com.uav.common.exception.GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Request processing exception: {} - {}", request.getDescription(false), ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", 500, "message", "Server internal error"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("code", 400, "message", ex.getMessage()));
    }
}
