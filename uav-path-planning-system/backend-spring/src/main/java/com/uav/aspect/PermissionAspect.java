package com.uav.aspect;

import com.uav.annotation.RequiresPermission;
import com.uav.common.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 权限检查切面
 * 实现基于注解的权限验证
 */
@Aspect
@Component
public class PermissionAspect {

    /**
     * 权限检查环绕通知
     */
    @Around("@annotation(com.uav.annotation.RequiresPermission) || @within(com.uav.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取注解
        RequiresPermission annotation = getAnnotation(joinPoint);
        
        if (annotation == null) {
            return joinPoint.proceed();
        }
        
        String[] requiredPermissions = annotation.value();
        boolean requireAll = annotation.requireAll();
        
        // 获取当前用户权限
        Set<String> userPermissions = getUserPermissions();
        
        // 权限检查
        if (!hasPermission(userPermissions, requiredPermissions, requireAll)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "权限不足，无法执行此操作");
        }
        
        return joinPoint.proceed();
    }
    
    /**
     * 获取注解
     */
    private RequiresPermission getAnnotation(ProceedingJoinPoint joinPoint) {
        // 先从方法获取
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission annotation = method.getAnnotation(RequiresPermission.class);
        
        // 如果方法没有，从类获取
        if (annotation == null) {
            Class<?> targetClass = joinPoint.getTarget().getClass();
            annotation = targetClass.getAnnotation(RequiresPermission.class);
        }
        
        return annotation;
    }
    
    /**
     * 获取当前用户的权限集合
     */
    private Set<String> getUserPermissions() {
        Set<String> permissions = new HashSet<>();
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                String authorityStr = authority.getAuthority();
                // 提取权限（去掉ROLE_前缀）
                if (authorityStr.startsWith("PERM_")) {
                    permissions.add(authorityStr.substring(5));
                }
            }
        }
        
        return permissions;
    }
    
    /**
     * 检查权限
     */
    private boolean hasPermission(Set<String> userPermissions, String[] requiredPermissions, boolean requireAll) {
        if (requiredPermissions == null || requiredPermissions.length == 0) {
            return true;
        }
        
        if (requireAll) {
            // 需要所有权限
            for (String permission : requiredPermissions) {
                if (!userPermissions.contains(permission)) {
                    return false;
                }
            }
            return true;
        } else {
            // 只要有一个权限即可
            for (String permission : requiredPermissions) {
                if (userPermissions.contains(permission)) {
                    return true;
                }
            }
            return false;
        }
    }
}