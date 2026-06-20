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

@DisplayName("ForecastRequest DTO验证测试")
class ForecastRequestTest {

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
        ForecastRequest request = new ForecastRequest();
        request.setMethod("lstm");
        request.setData(Map.of("lat", 39.9, "lon", 116.4));
        request.setConfig(Map.of("hours", 24));

        Set<ConstraintViolation<ForecastRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("测试空方法名")
    void testEmptyMethod() {
        ForecastRequest request = new ForecastRequest();
        request.setMethod("");
        request.setData(Map.of("lat", 39.9));
        request.setConfig(Map.of("hours", 24));

        Set<ConstraintViolation<ForecastRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("method")));
    }

    @Test
    @DisplayName("测试null方法名")
    void testNullMethod() {
        ForecastRequest request = new ForecastRequest();
        request.setMethod(null);
        request.setData(Map.of("lat", 39.9));
        request.setConfig(Map.of("hours", 24));

        Set<ConstraintViolation<ForecastRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("测试超长方法名")
    void testMethodTooLong() {
        ForecastRequest request = new ForecastRequest();
        request.setMethod("a".repeat(51));
        request.setData(Map.of("lat", 39.9));
        request.setConfig(Map.of("hours", 24));

        Set<ConstraintViolation<ForecastRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("测试null data")
    void testNullData() {
        ForecastRequest request = new ForecastRequest();
        request.setMethod("lstm");
        request.setData(null);
        request.setConfig(Map.of("hours", 24));

        Set<ConstraintViolation<ForecastRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }
}

@DisplayName("PathPlanningRequest DTO验证测试")
class PathPlanningRequestTest {

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
        PathPlanningRequest request = new PathPlanningRequest();
        request.setAlgorithm("vrptw");
        request.setDrones(Map.of("count", 3));
        request.setTasks(Map.of("start", "39.9,116.4", "end", "40.0,116.5"));
        request.setWeatherData(Map.of("wind", "5m/s"));
        request.setConstraints(Map.of("maxDistance", 100));

        Set<ConstraintViolation<PathPlanningRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("测试空算法")
    void testEmptyAlgorithm() {
        PathPlanningRequest request = new PathPlanningRequest();
        request.setAlgorithm("");

        Set<ConstraintViolation<PathPlanningRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("测试null算法")
    void testNullAlgorithm() {
        PathPlanningRequest request = new PathPlanningRequest();
        request.setAlgorithm(null);

        Set<ConstraintViolation<PathPlanningRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("测试超长算法名")
    void testAlgorithmTooLong() {
        PathPlanningRequest request = new PathPlanningRequest();
        request.setAlgorithm("a".repeat(51));

        Set<ConstraintViolation<PathPlanningRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("测试null drones")
    void testNullDrones() {
        PathPlanningRequest request = new PathPlanningRequest();
        request.setAlgorithm("vrptw");
        request.setDrones(null);

        Set<ConstraintViolation<PathPlanningRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }
}
