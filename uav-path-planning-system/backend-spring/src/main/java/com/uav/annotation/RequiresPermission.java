package com.uav.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限检查注解
 * 用于标注需要权限验证的方法或类
 * 
 * 使用示例:
 * @RequiresPermission("user:create")
 * public ResponseEntity<?> createUser(UserCreateRequest request) { ... }
 * 
 * @RequiresPermission({"user:read", "user:edit"})
 * public ResponseEntity<?> updateUser(Long id, UserUpdateRequest request) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    
    /**
     * 所需的权限编码
     * 格式: module:action
     * 例如: user:create, path:view
     */
    String[] value();
    
    /**
     * 是否需要所有权限（默认true）
     * true: 需要所有指定权限
     * false: 只要有一个权限即可
     */
    boolean requireAll() default true;
}