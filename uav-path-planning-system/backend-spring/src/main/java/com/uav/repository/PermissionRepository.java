package com.uav.repository;

import com.uav.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限数据访问层
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /**
     * 根据权限编码查找
     */
    Optional<Permission> findByCode(String code);

    /**
     * 根据模块查找权限
     */
    List<Permission> findByModule(String module);

    /**
     * 根据模块和操作类型查找
     */
    List<Permission> findByModuleAndAction(String module, String action);

    /**
     * 查找所有启用的权限
     */
    List<Permission> findByEnabledTrue();

    /**
     * 检查权限编码是否存在
     */
    boolean existsByCode(String code);
}