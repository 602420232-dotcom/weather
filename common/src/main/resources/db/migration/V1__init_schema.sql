-- ============================================================
-- V1.0.0: 初始化数据库Schema - 用户与认证
-- ============================================================

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100),
    `full_name` VARCHAR(100),
    `enabled` TINYINT(1) DEFAULT 1,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `roles` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(50) NOT NULL UNIQUE,
    `description` VARCHAR(255)
);

INSERT INTO `roles` (`name`, `description`) VALUES
    ('ROLE_USER', '普通用户'),
    ('ROLE_ADMIN', '管理员')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);

CREATE TABLE IF NOT EXISTS `user_roles` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`role_id`) REFERENCES `roles`(`id`) ON DELETE CASCADE
);

-- ============================================================
-- V1.0.1: 核心业务表 - 气象数据与路径规划
-- ============================================================

CREATE TABLE IF NOT EXISTS `weather_data` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `source` VARCHAR(20) NOT NULL COMMENT '数据源: wrf/satellite/station/buoy',
    `drone_id` VARCHAR(50),
    `latitude` DOUBLE NOT NULL,
    `longitude` DOUBLE NOT NULL,
    `altitude` DOUBLE,
    `temperature` DOUBLE,
    `humidity` DOUBLE,
    `wind_speed` DOUBLE,
    `wind_direction` DOUBLE,
    `wind_gust` DOUBLE,
    `pressure` DOUBLE,
    `data` JSON,
    `recorded_at` TIMESTAMP NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_source` (`source`),
    INDEX `idx_recorded_at` (`recorded_at`),
    INDEX `idx_source_recorded` (`source`, `recorded_at`)
);

CREATE TABLE IF NOT EXISTS `path_plans` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `drone_id` VARCHAR(50) NOT NULL,
    `algorithm` VARCHAR(50) NOT NULL COMMENT 'vrptw/astar/dwa',
    `status` VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/running/completed/failed',
    `waypoints` JSON,
    `weather_snapshot` JSON,
    `total_distance` DOUBLE,
    `estimated_time` DOUBLE,
    `started_at` TIMESTAMP,
    `completed_at` TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_drone_id` (`drone_id`),
    INDEX `idx_status` (`status`)
);

-- ============================================================
-- V1.0.2: 数据源管理与审计日志
-- ============================================================

CREATE TABLE IF NOT EXISTS `data_sources` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `type` VARCHAR(50) NOT NULL COMMENT 'ground_station/buoy/satellite/weather_station',
    `url` VARCHAR(500),
    `status` VARCHAR(20) DEFAULT 'active',
    `last_checked` TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50),
    `action` VARCHAR(100) NOT NULL,
    `resource` VARCHAR(200),
    `detail` TEXT,
    `ip_address` VARCHAR(45),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`),
    INDEX `idx_action` (`action`),
    INDEX `idx_created_at` (`created_at`)
);
