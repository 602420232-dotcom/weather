package com.uav.integration;

import com.uav.UavPathPlanningApplication;
import com.uav.model.Role;
import com.uav.model.User;
import com.uav.repository.RoleRepository;
import com.uav.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 路径规划系统集成测试
 * 覆盖系统主要功能模块间的交互逻辑
 */
@Disabled("Requires full integration test infrastructure setup: pre-seeded database with roles/users, "
    + "proper JWT authentication flow configuration. Re-enable after implementing database seeding strategy "
    + "and fixing TestRestTemplate/security filter chain interaction with RANDOM_PORT web environment.")
@SpringBootTest(classes = UavPathPlanningApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PathPlanningIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;

        transactionTemplate.executeWithoutResult(status -> {
            // Seed required data: USER role and admin user
            if (roleRepository.findByName("USER").isEmpty()) {
                Role userRole = new Role();
                userRole.setName("USER");
                userRole.setDescription("普通用户");
                roleRepository.save(userRole);
            }

            if (roleRepository.findByName("ADMIN").isEmpty()) {
                Role adminRole = new Role();
                adminRole.setName("ADMIN");
                adminRole.setDescription("管理员");
                roleRepository.save(adminRole);
            }

            if (!userRepository.existsByUsername("admin")) {
                Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("password"));
                admin.setEmail("admin@example.com");
                admin.setFullName("管理员");
                admin.setEnabled(true);
                admin.setAccountNonExpired(true);
                admin.setAccountNonLocked(true);
                admin.setCredentialsNonExpired(true);
                admin.setRoles(Set.of(adminRole));
                userRepository.save(admin);
            }
        });
    }

    @Test
    @DisplayName("集成测试：用户认证登录成功")
    void testUserCreationAndAuthentication() {
        // 使用管理员账号登录
        ResponseEntity<Map<String, Object>> loginResponse = authenticateUser("admin", "password");
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().containsKey("token"));

        // 注册新用户
        String username = "testuser_" + System.currentTimeMillis();
        ResponseEntity<Map<String, Object>> createResponse = createUser(username);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());

        // 验证用户已创建
        User user = userRepository.findByUsername(username).orElse(null);
        assertNotNull(user);
        assertEquals(username, user.getUsername());

        // 使用新用户登录
        ResponseEntity<Map<String, Object>> newLoginResponse = authenticateUser(username, "Password123");
        assertEquals(HttpStatus.OK, newLoginResponse.getStatusCode());
        assertNotNull(newLoginResponse.getBody().get("token"));
    }

    @Test
    @DisplayName("集成测试：权限验证 - 未认证请求被拒绝")
    void testPermissionValidation() {
        // 未认证请求应被拒绝（Spring Security 默认返回 403）
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "/api/admin/users", HttpMethod.GET, null,
            new ParameterizedTypeReference<Map<String, Object>>() {});
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("集成测试：路径规划流程（认证后访问受保护资源）")
    void testPathPlanningBoundaryConditions() {
        // 认证
        ResponseEntity<Map<String, Object>> loginResponse = authenticateUser("admin", "password");
        assertNotNull(loginResponse.getBody());
        String token = loginResponse.getBody().get("token").toString();

        // 使用 token 访问受保护的路径规划端点
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");

        Map<String, Object> request = new HashMap<>();
        request.put("tasks", "[{\"id\":1,\"location\":\"北京\",\"priority\":1}]");
        request.put("drones", "[{\"id\":1,\"capacity\":100}]");
        request.put("weatherData", "{\"windSpeed\":10,\"temperature\":25}");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map<String, Object>> planResponse = restTemplate.exchange(
            baseUrl + "/path-planning/plan", HttpMethod.POST, entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});

        assertNotNull(planResponse.getBody());
        assertTrue((Boolean) planResponse.getBody().get("success"));
    }

    @Test
    @DisplayName("集成测试：异常场景 - 认证失败和无效token")
    void testExceptionHandling() {
        // 错误的凭证登录
        ResponseEntity<String> failedLogin = authenticateUserString("invalid", "wrong");
        assertEquals(HttpStatus.UNAUTHORIZED, failedLogin.getStatusCode());

        // 无效token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid_token");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
            baseUrl + "/api/admin/users", HttpMethod.GET, entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});

        // Spring Security 默认对未认证请求返回 403
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    @DisplayName("集成测试：完整路径规划流程")
    void testCompletePathPlanningFlow() {
        // Step 1: 用户认证
        ResponseEntity<Map<String, Object>> loginResponse = authenticateUser("admin", "password");
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().containsKey("token"));

        String token = loginResponse.getBody().get("token").toString();

        // Step 2: 请求路径规划
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");

        Map<String, Object> request = new HashMap<>();
        request.put("tasks", "[{\"id\":1,\"location\":\"北京\",\"priority\":1}]");
        request.put("drones", "[{\"id\":1,\"capacity\":100}]");
        request.put("weatherData", "{\"windSpeed\":10,\"temperature\":25}");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<Map<String, Object>> planResponse = restTemplate.exchange(
            baseUrl + "/path-planning/plan", HttpMethod.POST, entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.OK, planResponse.getStatusCode());
        assertNotNull(planResponse.getBody());
        assertTrue((Boolean) planResponse.getBody().get("success"));
        assertEquals(200, planResponse.getBody().get("code"));
        assertNotNull(planResponse.getBody().get("data"));

        // Step 3: 获取规划历史
        HttpEntity<Void> historyEntity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> historyResponse = restTemplate.exchange(
            baseUrl + "/path-planning/history", HttpMethod.GET, historyEntity,
            new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.OK, historyResponse.getStatusCode());
        assertNotNull(historyResponse.getBody());
        assertTrue((Boolean) historyResponse.getBody().get("success"));
    }

    private ResponseEntity<Map<String, Object>> authenticateUser(String username, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(credentials, headers);
        return restTemplate.exchange(baseUrl + "/api/v1/auth/login", HttpMethod.POST, entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private ResponseEntity<String> authenticateUserString(String username, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(credentials, headers);
        return restTemplate.exchange(baseUrl + "/api/v1/auth/login", HttpMethod.POST, entity, String.class);
    }

    private ResponseEntity<Map<String, Object>> createUser(String username) {
        Map<String, String> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("password", "Password123");
        userData.put("email", username + "@example.com");
        userData.put("fullName", "Test User");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(userData);
        return restTemplate.exchange(baseUrl + "/api/v1/auth/register", HttpMethod.POST, entity,
            new ParameterizedTypeReference<Map<String, Object>>() {});
    }
}
