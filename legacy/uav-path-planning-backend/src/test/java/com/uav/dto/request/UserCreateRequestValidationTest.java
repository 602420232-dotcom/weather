package com.uav.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UserCreateRequest参数校验测试
 */
class UserCreateRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("有效请求通过校验")
    void validRequest_PassesValidation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("test_user");
        request.setPassword("Password123");
        request.setEmail("test@example.com");
        request.setFullName("Test User");

        Set<jakarta.validation.ConstraintViolation<UserCreateRequest>> violations = 
            validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("用户名太短触发校验失败")
    void usernameTooShort_FailsValidation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("ab"); // 小于3个字符
        request.setPassword("Password123");
        request.setEmail("test@example.com");
        request.setFullName("Test User");

        Set<jakarta.validation.ConstraintViolation<UserCreateRequest>> violations = 
            validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("用户名包含特殊字符触发校验失败")
    void usernameWithSpecialChars_FailsValidation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("test@user"); // 包含@
        request.setPassword("Password123");
        request.setEmail("test@example.com");
        request.setFullName("Test User");

        Set<jakarta.validation.ConstraintViolation<UserCreateRequest>> violations = 
            validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    @DisplayName("密码不符合规则触发校验失败")
    void passwordWithoutUpperCase_FailsValidation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("test_user");
        request.setPassword("password123"); // 缺少大写字母
        request.setEmail("test@example.com");
        request.setFullName("Test User");

        Set<jakarta.validation.ConstraintViolation<UserCreateRequest>> violations = 
            validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    @DisplayName("邮箱格式错误触发校验失败")
    void invalidEmailFormat_FailsValidation() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("test_user");
        request.setPassword("Password123");
        request.setEmail("invalid-email"); // 无效邮箱
        request.setFullName("Test User");

        Set<jakarta.validation.ConstraintViolation<UserCreateRequest>> violations = 
            validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("空字段触发校验失败")
    void emptyFields_FailValidation() {
        UserCreateRequest request = new UserCreateRequest();
        // 所有字段为空

        Set<jakarta.validation.ConstraintViolation<UserCreateRequest>> violations = 
            validator.validate(request);

        assertFalse(violations.isEmpty());
        assertEquals(4, violations.size()); // 4个必填字段
    }
}