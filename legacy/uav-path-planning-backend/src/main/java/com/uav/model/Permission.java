package com.uav.model;

import lombok.Data;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Set;

/**
 * 权限实体
 * 支持模块级和操作级的细粒度权限控制
 */
@Data
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 权限标识（唯一）
     * 格式: module:action
     * 例如: user:create, path:view, drone:edit
     */
    @Column(unique = true, nullable = false)
    private String code;

    /**
     * 权限名称（显示用）
     */
    @Column(nullable = false)
    private String name;

    /**
     * 权限描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 所属模块
     * 例如: user, path, drone, weather
     */
    @Column(nullable = false)
    private String module;

    /**
     * 操作类型
     * 例如: create, read, update, delete, view
     */
    @Column(nullable = false)
    private String action;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * 关联的角色
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "permission_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;
}