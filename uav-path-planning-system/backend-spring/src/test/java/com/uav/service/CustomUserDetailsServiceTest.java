package com.uav.service;

import com.uav.model.Permission;
import com.uav.model.Role;
import com.uav.model.User;
import com.uav.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * CustomUserDetailsService单元测试
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository);
    }

    @Test
    @DisplayName("加载用户成功 - 包含角色和权限")
    void loadUserByUsername_Success() {
        // 准备测试数据
        Permission permission1 = new Permission();
        permission1.setId(1L);
        permission1.setCode("user:read");
        permission1.setName("用户查询");
        permission1.setModule("user");
        permission1.setAction("read");
        permission1.setEnabled(true);

        Permission permission2 = new Permission();
        permission2.setId(2L);
        permission2.setCode("user:create");
        permission2.setName("用户创建");
        permission2.setModule("user");
        permission2.setAction("create");
        permission2.setEnabled(true);

        Set<Permission> permissions = new HashSet<>();
        permissions.add(permission1);
        permissions.add(permission2);

        Role role = new Role();
        role.setId(1L);
        role.setName("ADMIN");
        role.setDescription("管理员");
        role.setPermissions(permissions);

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        user.setRoles(roles);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        // 执行测试
        UserDetails result = service.loadUserByUsername("testuser");

        // 验证结果
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.isAccountNonExpired());
        assertTrue(result.isAccountNonLocked());
        assertTrue(result.isCredentialsNonExpired());

        // 验证权限
        var authorities = result.getAuthorities();
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("PERM_user:read")));
        assertTrue(authorities.contains(new SimpleGrantedAuthority("PERM_user:create")));
    }

    @Test
    @DisplayName("用户不存在时抛出异常")
    void loadUserByUsername_UserNotFound() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> 
            service.loadUserByUsername("nonexistent"));
    }

    @Test
    @DisplayName("禁用的权限不会被加载")
    void loadUserByUsername_DisabledPermissionNotLoaded() {
        Permission disabledPermission = new Permission();
        disabledPermission.setId(1L);
        disabledPermission.setCode("user:delete");
        disabledPermission.setName("用户删除");
        disabledPermission.setModule("user");
        disabledPermission.setAction("delete");
        disabledPermission.setEnabled(false);

        Set<Permission> permissions = new HashSet<>();
        permissions.add(disabledPermission);

        Role role = new Role();
        role.setId(1L);
        role.setName("USER");
        role.setDescription("普通用户");
        role.setPermissions(permissions);

        Set<Role> roles = new HashSet<>();
        roles.add(role);

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encodedPassword");
        user.setRoles(roles);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("testuser");

        // 验证禁用的权限不会被加载
        var authorities = result.getAuthorities();
        assertFalse(authorities.contains(new SimpleGrantedAuthority("PERM_user:delete")));
    }
}