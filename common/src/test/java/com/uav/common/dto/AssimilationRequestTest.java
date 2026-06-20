package com.uav.common.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssimilationRequest验证测试")
class AssimilationRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("测试有效请求")
    void testValidRequest() {
        AssimilationRequest request = new AssimilationRequest();
        request.setAlgorithm("3dvar");
        request.setBackground(Map.of("domain", "1000x1000"));
        request.setObservations(Map.of("stations", 10));
        request.setConfig(Map.of("resolution", 50.0));

        Set<ConstraintViolation<AssimilationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("测试空算法名")
    void testEmptyAlgorithm() {
        AssimilationRequest request = new AssimilationRequest();
        request.setAlgorithm("");
        request.setBackground(Map.of("domain", "1000x1000"));
        request.setObservations(Map.of("stations", 10));

        Set<ConstraintViolation<AssimilationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("测试null算法名")
    void testNullAlgorithm() {
        AssimilationRequest request = new AssimilationRequest();
        request.setAlgorithm(null);
        request.setBackground(Map.of("domain", "1000x1000"));

        Set<ConstraintViolation<AssimilationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("测试超长算法名")
    void testAlgorithmTooLong() {
        AssimilationRequest request = new AssimilationRequest();
        request.setAlgorithm("a".repeat(51));

        Set<ConstraintViolation<AssimilationRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("测试有效无背景数据")
    void testNullBackground() {
        AssimilationRequest request = new AssimilationRequest();
        request.setAlgorithm("3dvar");
        request.setBackground(null);
        request.setObservations(Map.of("stations", 10));

        Set<ConstraintViolation<AssimilationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("测试null观测数据")
    void testNullObservations() {
        AssimilationRequest request = new AssimilationRequest();
        request.setAlgorithm("3dvar");
        request.setBackground(Map.of("domain", "1000x1000"));
        request.setObservations(null);

        Set<ConstraintViolation<AssimilationRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }
}
