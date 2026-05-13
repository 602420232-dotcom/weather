package com.uav.controller;

import com.uav.model.Role;
import com.uav.model.User;
import com.uav.repository.RoleRepository;
import com.uav.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@DisplayName("UserController 用户管理集成测试")
class UserControllerTest {

    @Autowired
    private UserController userController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        if (roleRepository.count() == 0) {
            Role adminRole = new Role();
            adminRole.setName("ADMIN");
            roleRepository.save(adminRole);
            Role userRole = new Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }
    }

    @Test
    @DisplayName("创建新用户")
    void testCreateUser() {
        User newUser = new User();
        newUser.setUsername("operator");
        newUser.setPassword("password123");
        newUser.setFullName("操作员");

        User created = userController.createUser(newUser);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("operator", created.getUsername());
    }

    @Test
    @DisplayName("获取用户列表")
    void testGetUsers() {
        User u = new User();
        u.setUsername("test1");
        u.setPassword("pass");
        u.setFullName("测试1");
        userRepository.save(u);

        List<User> users = userController.getUsers();
        assertFalse(users.isEmpty());
    }

    @Test
    @DisplayName("按ID获取用户")
    void testGetUserById() {
        User u = new User();
        u.setUsername("tester");
        u.setPassword("pass");
        u.setFullName("测试者");
        User saved = userRepository.save(u);

        User result = userController.getUser(saved.getId());
        assertNotNull(result);
        assertEquals("tester", result.getUsername());
    }

    @Test
    @DisplayName("获取不存在的用户返回null")
    void testGetNonExistentUser() {
        User user = userController.getUser(999L);
        assertNull(user);
    }

    @Test
    @DisplayName("删除用户")
    void testDeleteUser() {
        User u = new User();
        u.setUsername("toDelete");
        u.setPassword("pass");
        u.setFullName("待删除");
        User saved = userRepository.save(u);

        assertTrue(userController.deleteUser(saved.getId()));
        assertNull(userController.getUser(saved.getId()));
    }

    @Test
    @DisplayName("删除不存在用户返回false")
    void testDeleteNonExistentUser() {
        assertFalse(userController.deleteUser(999L));
    }
}
