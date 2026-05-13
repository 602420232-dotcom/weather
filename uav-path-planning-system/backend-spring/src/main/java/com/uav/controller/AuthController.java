package com.uav.controller;

import com.uav.common.exception.BusinessException;
import com.uav.config.JwtUtil;
import com.uav.config.SecurityAuditConfig;
import com.uav.model.Role;
import com.uav.model.User;
import com.uav.repository.RoleRepository;
import com.uav.repository.UserRepository;
import com.uav.service.CustomUserDetailsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditConfig securityAuditConfig;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JwtUtil jwtUtil,
                          PasswordEncoder passwordEncoder,
                          SecurityAuditConfig securityAuditConfig,
                          UserRepository userRepository,
                          RoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.securityAuditConfig = securityAuditConfig;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        public String username;
        @NotBlank(message = "密码不能为空")
        public String password;
    }

    public static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度3-50字符")
        public String username;
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 100, message = "密码长度6-100字符")
        public String password;
        @Email(message = "邮箱格式不正确")
        public String email;
        public String fullName;
    }

    public static class RefreshTokenRequest {
        @NotBlank(message = "刷新令牌不能为空")
        public String token;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.username;
        String password = request.password;

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String token = jwtUtil.generateToken(userDetails);

            securityAuditConfig.logAuthenticationSuccess(username, httpRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "登录成功");
            response.put("data", Map.of("token", token));

            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            securityAuditConfig.logAuthenticationFailure(username, "凭证错误", httpRequest);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", 401, "message", "用户名或密码错误"));
        } catch (DisabledException e) {
            securityAuditConfig.logAuthenticationFailure(username, "账户已禁用", httpRequest);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code", 403, "message", "账户已被禁用"));
        } catch (LockedException e) {
            securityAuditConfig.logAuthenticationFailure(username, "账户已锁定", httpRequest);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("code", 403, "message", "账户已被锁定"));
        } catch (UsernameNotFoundException e) {
            securityAuditConfig.logAuthenticationFailure(username, "凭证错误", httpRequest);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", 401, "message", "用户名或密码错误"));
        } catch (AuthenticationException e) {
            securityAuditConfig.logAuthenticationFailure(username, "认证失败", httpRequest);
            throw new BusinessException("AUTH_ERROR", "登录失败，请稍后重试");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByUsername(request.username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", 400, "message", "用户名已存在"));
        }

        User user = new User();
        user.setUsername(request.username);
        user.setPassword(passwordEncoder.encode(request.password));
        user.setEmail(request.email);
        user.setFullName(request.fullName != null ? request.fullName : request.username);
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName("USER");
                    return roleRepository.save(r);
                });
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        securityAuditConfig.logUserActivity(savedUser.getUsername(), "注册", "新用户注册成功");

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "注册成功");
        response.put("data", Map.of("token", token));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String token = request.token;

        try {
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtUtil.validateToken(token, userDetails)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", 401, "message", "令牌无效或已过期"));
            }

            String newToken = jwtUtil.generateToken(userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "令牌刷新成功");
            response.put("data", Map.of("token", newToken));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("code", 401, "message", "令牌无效"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        String username = securityAuditConfig.getCurrentUsername();
        securityAuditConfig.logUserActivity(username, "登出", "用户主动登出");
        return ResponseEntity.ok(Map.of("code", 200, "message", "登出成功"));
    }
}
