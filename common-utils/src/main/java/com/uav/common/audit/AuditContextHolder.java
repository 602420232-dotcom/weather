package com.uav.common.audit;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class AuditContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static SecurityAuditService getSecurityAuditService() {
        if (context == null) {
            throw new IllegalStateException(
                    "Spring ApplicationContext not initialized. "
                    + "Ensure AuditContextHolder is registered as a Spring bean.");
        }
        return context.getBean(SecurityAuditService.class);
    }
}
