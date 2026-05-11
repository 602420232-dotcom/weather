package com.uav.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimitConfig {

    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .flatMap(addr -> Optional.ofNullable(addr.getAddress()))
                .map(addr -> addr.getHostAddress())
                .orElse("unknown")
        );
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
            Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-User-Id"))
                .filter(id -> !id.isEmpty())
                .orElse("anonymous")
        );
    }
}