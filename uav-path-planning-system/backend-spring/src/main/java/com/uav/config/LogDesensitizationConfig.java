package com.uav.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 日志脱敏配置
 * 对敏感信息进行统一脱敏处理
 */
@Configuration
public class LogDesensitizationConfig {

    /**
     * 脱敏规则映射
     */
    private static final Map<String, Pattern> DESENSITIZATION_PATTERNS = new HashMap<>();
    
    static {
        // 身份证号脱敏：保留前4位和后4位
        DESENSITIZATION_PATTERNS.put("idCard", Pattern.compile("(\\d{4})\\d{10}(\\d{4})"));
        
        // 手机号脱敏：保留前3位和后4位
        DESENSITIZATION_PATTERNS.put("phone", Pattern.compile("(\\d{3})\\d{4}(\\d{4})"));
        
        // 邮箱脱敏：保留@前1位和域名
        DESENSITIZATION_PATTERNS.put("email", Pattern.compile("(\\w{1})\\w+@(\\w+\\.\\w+)"));
        
        // 银行卡号脱敏：保留前6位和后4位
        DESENSITIZATION_PATTERNS.put("bankCard", Pattern.compile("(\\d{6})\\d{8,10}(\\d{4})"));
        
        // 密码脱敏：全部替换为***
        DESENSITIZATION_PATTERNS.put("password", Pattern.compile("\"password\"\\s*:\\s*\"[^\"]+\""));
    }

    /**
     * 脱敏处理消息转换器
     */
    public static class DesensitizationMessageConverter extends MessageConverter {
        
        @Override
        public String convert(ILoggingEvent event) {
            String message = super.convert(event);
            return desensitize(message);
        }
    }

    /**
     * 脱敏处理方法
     */
    public static String desensitize(String message) {
        if (message == null) {
            return null;
        }
        
        String result = message;
        
        // 身份证号脱敏
        result = DESENSITIZATION_PATTERNS.get("idCard").matcher(result).replaceAll("$1**********$2");
        
        // 手机号脱敏
        result = DESENSITIZATION_PATTERNS.get("phone").matcher(result).replaceAll("$1****$2");
        
        // 邮箱脱敏
        result = DESENSITIZATION_PATTERNS.get("email").matcher(result).replaceAll("$1****@$2");
        
        // 银行卡号脱敏
        result = DESENSITIZATION_PATTERNS.get("bankCard").matcher(result).replaceAll("$1********$2");
        
        // 密码脱敏
        result = DESENSITIZATION_PATTERNS.get("password").matcher(result).replaceAll("\"password\": \"***\"");
        
        return result;
    }
}