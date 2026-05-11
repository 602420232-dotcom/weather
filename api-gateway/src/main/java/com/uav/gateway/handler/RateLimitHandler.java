package com.uav.gateway.handler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RateLimitHandler {

    private final ObjectMapper objectMapper;

    public RateLimitHandler(@NonNull ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @NonNull
    private byte[] createResponseBytes(@NonNull Map<String, Object> body) {
        try {
            return Objects.requireNonNull(objectMapper.writeValueAsBytes(body));
        } catch (JsonProcessingException e) {
            return Objects.requireNonNull(
                    "{\"code\":429,\"message\":\"Rate limit exceeded\"}".getBytes(StandardCharsets.UTF_8));
        }
    }

    public Mono<Void> handle(@NonNull ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> body = new HashMap<>();
        body.put("code", 429);
        body.put("message", "请求过于频繁，请稍后重试");
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("retry-after", 60);
        
        byte[] bytes = createResponseBytes(body);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        Publisher<? extends DataBuffer> publisher = Objects.requireNonNull(Mono.just(buffer));
        return exchange.getResponse().writeWith(publisher);
    }
}