package com.uav.controller;

import com.uav.model.Role;
import com.uav.model.User;
import com.uav.repository.RoleRepository;
import com.uav.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api")
public class UserController {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${app.default-password:}")
    private String defaultPasswordRaw;

    @Value("${app.init-default-users:false}")
    private boolean initDefaultUsers;

    public UserController(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @PostConstruct
    public void init() {
        if (!initDefaultUsers) {
            return;
        }
        String resolvedPassword = defaultPasswordRaw;
        if (resolvedPassword == null || resolvedPassword.isEmpty()) {
            resolvedPassword = System.getenv("APP_DEFAULT_ADMIN_PASSWORD");
        }
        if (resolvedPassword == null || resolvedPassword.isEmpty()) {
            log.warn("默认管理员密码未配置，跳过初始化默认用户");
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ADMIN");
            r.setDescription("系统管理员");
            return roleRepository.save(r);
        });
        Role dispatcherRole = roleRepository.findByName("DISPATCHER").orElseGet(() -> {
            Role r = new Role();
            r.setName("DISPATCHER");
            r.setDescription("调度员");
            return roleRepository.save(r);
        });
        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("USER");
            r.setDescription("普通用户");
            return roleRepository.save(r);
        });

        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(resolvedPassword));
            admin.setFullName("管理员");
            admin.setEmail("admin@example.com");
            admin.setRoles(new HashSet<>(Set.of(adminRole, userRole)));
            userRepository.save(admin);
            log.info("默认管理员用户已创建");
        }

        if (!userRepository.existsByUsername("dispatcher")) {
            User dispatcher = new User();
            dispatcher.setUsername("dispatcher");
            dispatcher.setPassword(passwordEncoder.encode(resolvedPassword));
            dispatcher.setFullName("调度员");
            dispatcher.setEmail("dispatcher@example.com");
            dispatcher.setRoles(new HashSet<>(Set.of(dispatcherRole, userRole)));
            userRepository.save(dispatcher);
            log.info("默认调度员用户已创建");
        }
    }

    @GetMapping("/admin/users")
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/admin/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @PostMapping("/admin/users")
    public User createUser(@RequestBody User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @PutMapping("/admin/users/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userRepository.findById(id).map(existingUser -> {
            if (user.getUsername() != null) existingUser.setUsername(user.getUsername());
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            if (user.getEmail() != null) existingUser.setEmail(user.getEmail());
            if (user.getFullName() != null) existingUser.setFullName(user.getFullName());
            if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                existingUser.setRoles(user.getRoles());
            }
            existingUser.setEnabled(user.isEnabled());
            return userRepository.save(existingUser);
        }).orElse(null);
    }

    @DeleteMapping("/admin/users/{id}")
    public boolean deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
