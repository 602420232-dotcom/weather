package com.uav.gateway.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class RateLimitHandler {

    private final ObjectMapper objectMapper;

    public RateLimitHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("null")
    public Mono<Void> handle(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = new HashMap<>();
        body.put("code", 429);
        body.put("message", "请求过于频繁，请稍后重试");
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("retry-after", 60);
        
        byte[] bytes = createResponseBytes(body);
        @SuppressWarnings("null")
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
    
    private byte[] createResponseBytes(Map<String, Object> body) {
        try {
            byte[] result = objectMapper.writeValueAsBytes(body);
            return (result != null) ? result : "{}".getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            return "{\"code\":429,\"message\":\"Rate limit exceeded\"}".getBytes(StandardCharsets.UTF_8);
        }
    }
}