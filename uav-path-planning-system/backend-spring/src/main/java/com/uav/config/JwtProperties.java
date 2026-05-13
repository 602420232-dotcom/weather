package com.uav.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long expiration = 86400000;
    private boolean enabled = true;

    @PostConstruct
    public void validate() {
        if (enabled) {
            if (secret == null || secret.isEmpty()) {
                String envSecret = System.getenv("JWT_SECRET");
                if (envSecret != null && !envSecret.isEmpty()) {
                    secret = envSecret;
                } else {
                    throw new IllegalStateException(
                        "JWT密钥未配置。请设置环境变量 JWT_SECRET 或配置项 jwt.secret。"
                        + "可以使用: openssl rand -base64 64 生成安全密钥");
                }
            }
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
