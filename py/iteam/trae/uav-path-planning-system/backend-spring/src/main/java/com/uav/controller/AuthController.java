package com.uav.controller;

import com.uav.config.JwtUtil;
import com.uav.config.SecurityAuditConfig;
import com.uav.model.User;
import com.uav.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String username = request.get("username");
        String password = request.get("password");
        
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String token = jwtUtil.generateToken(userDetails);
            
            // 记录登录成功
            SecurityAuditConfig.logAuthenticationSuccess(username, httpRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", userDetails);
            
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            // 记录登录失败
            SecurityAuditConfig.logAuthenticationFailure(username, "密码错误", httpRequest);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        } catch (Exception e) {
            // 记录登录失败
            SecurityAuditConfig.logAuthenticationFailure(username, e.getMessage(), httpRequest);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("登录失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String username = request.get("username");
        String password = request.get("password");
        String email = request.get("email");
        String fullName = request.get("fullName");
        
        try {
            // 检查用户名是否已存在
            if (userDetailsService.loadUserByUsername(username) != null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("用户名已存在");
            }
            
            // 创建新用户
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setEmail(email);
            user.setFullName(fullName);
            user.setEnabled(true);
            user.setAccountNonExpired(true);
            user.setAccountNonLocked(true);
            user.setCredentialsNonExpired(true);
            
            // 保存用户
            // TODO: 实现用户保存逻辑
            
            // 记录注册成功
            SecurityAuditConfig.logUserActivity(username, "注册", "新用户注册成功");
            
            return ResponseEntity.ok("注册成功");
        } catch (Exception e) {
            // 记录注册失败
            SecurityAuditConfig.logUserActivity(username, "注册失败", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("注册失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String refreshToken = request.get("refreshToken");
        
        try {
            // 验证刷新令牌
            // TODO: 实现刷新令牌逻辑
            
            // 生成新令牌
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String newToken = jwtUtil.generateToken(userDetails);
            
            // 记录令牌刷新
            SecurityAuditConfig.logUserActivity(username, "刷新令牌", "令牌刷新成功");
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // 记录令牌刷新失败
            SecurityAuditConfig.logUserActivity("unknown", "刷新令牌失败", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("刷新令牌无效");
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        String username = SecurityAuditConfig.getCurrentUsername();
        
        // 记录登出
        SecurityAuditConfig.logUserActivity(username, "登出", "用户主动登出");
        
        return ResponseEntity.ok("登出成功");
    }
}