package com.uav.controller;

import com.uav.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    // 模拟用户数据
    private List<User> users = new ArrayList<>();
    
    public UserController() {
        // 初始化模拟数据
        User admin = new User();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setRole("ADMIN");
        admin.setName("管理员");
        admin.setEmail("admin@example.com");
        admin.setPhone("13800138000");
        
        User dispatcher = new User();
        dispatcher.setId(2L);
        dispatcher.setUsername("dispatcher");
        dispatcher.setPassword(passwordEncoder.encode("admin"));
        dispatcher.setRole("DISPATCHER");
        dispatcher.setName("调度员");
        dispatcher.setEmail("dispatcher@example.com");
        dispatcher.setPhone("13800138001");
        
        User operator = new User();
        operator.setId(3L);
        operator.setUsername("operator");
        operator.setPassword(passwordEncoder.encode("admin"));
        operator.setRole("OPERATOR");
        operator.setName("操作员");
        operator.setEmail("operator@example.com");
        operator.setPhone("13800138002");
        
        User user = new User();
        user.setId(4L);
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("admin"));
        user.setRole("USER");
        user.setName("普通用户");
        user.setEmail("user@example.com");
        user.setPhone("13800138003");
        
        users.add(admin);
        users.add(dispatcher);
        users.add(operator);
        users.add(user);
    }
    
    /**
     * 获取所有用户
     * @return 用户列表
     */
    @GetMapping("/admin/users")
    public List<User> getUsers() {
        return users;
    }
    
    /**
     * 获取用户详情
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/admin/users/{id}")
    public User getUser(@PathVariable Long id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
    }
    
    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建结果
     */
    @PostMapping("/admin/users")
    public User createUser(@RequestBody User user) {
        user.setId((long) (users.size() + 1));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        users.add(user);
        return user;
    }
    
    /**
     * 更新用户
     * @param id 用户ID
     * @param user 用户信息
     * @return 更新结果
     */
    @PutMapping("/admin/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        User existingUser = users.stream().filter(u -> u.getId().equals(id)).findFirst().orElse(null);
        if (existingUser != null) {
            existingUser.setUsername(user.getUsername());
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            existingUser.setRole(user.getRole());
            existingUser.setName(user.getName());
            existingUser.setEmail(user.getEmail());
            existingUser.setPhone(user.getPhone());
        }
        return existingUser;
    }
    
    /**
     * 删除用户
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/admin/users/{id}")
    public boolean deleteUser(@PathVariable Long id) {
        return users.removeIf(u -> u.getId().equals(id));
    }
}