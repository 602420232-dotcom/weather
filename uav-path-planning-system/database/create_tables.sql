-- 创建用户表
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `email` VARCHAR(100) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建无人机表
CREATE TABLE `drone` (
  `id` VARCHAR(20) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `max_payload` DOUBLE NOT NULL,
  `max_endurance` INT NOT NULL,
  `max_speed` DOUBLE NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `location` VARCHAR(100) NOT NULL,
  `battery` INT NOT NULL,
  `description` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建任务表
CREATE TABLE `task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `location` VARCHAR(100) NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `priority` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `description` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建气象数据表
CREATE TABLE `weather_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `time` DATETIME NOT NULL,
  `height` INT NOT NULL,
  `wind_speed` DOUBLE NOT NULL,
  `wind_direction` DOUBLE NOT NULL,
  `temperature` DOUBLE NOT NULL,
  `humidity` DOUBLE NOT NULL,
  `turbulence` VARCHAR(20) NOT NULL,
  `visibility` DOUBLE NOT NULL,
  `risk` VARCHAR(20) NOT NULL,
  `data` JSON NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_time_height` (`time`, `height`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建路径规划表
CREATE TABLE `path_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` BIGINT NOT NULL,
  `drone_id` VARCHAR(20) NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `total_distance` DOUBLE NOT NULL,
  `total_time` DOUBLE NOT NULL,
  `route` JSON NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`task_id`) REFERENCES `task`(`id`),
  FOREIGN KEY (`drone_id`) REFERENCES `drone`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建操作日志表
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `operation` VARCHAR(100) NOT NULL,
  `target` VARCHAR(100) NOT NULL,
  `target_id` VARCHAR(100) NOT NULL,
  `details` TEXT,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入初始用户数据
INSERT INTO `user` (`username`, `password`, `role`, `name`, `email`, `phone`) VALUES
('admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'admin', '管理员', 'admin@example.com', '13800138000'),
('dispatcher', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'dispatcher', '调度员', 'dispatcher@example.com', '13800138001'),
('operator', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'operator', '操作员', 'operator@example.com', '13800138002'),
('user', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'user', '普通用户', 'user@example.com', '13800138003');

-- 插入初始无人机数据
INSERT INTO `drone` (`id`, `name`, `type`, `max_payload`, `max_endurance`, `max_speed`, `status`, `location`, `battery`, `description`) VALUES
('UAV-001', '无人机1', 'multirotor', 5, 60, 50, '在线', '39.9042, 116.4074', 85, '配送无人机'),
('UAV-002', '无人机2', 'multirotor', 10, 45, 40, '执行任务', '39.9142, 116.4174', 60, '巡检无人机'),
('UAV-003', '无人机3', 'fixed-wing', 20, 120, 80, '待命', '39.9042, 116.4074', 90, '测绘无人机');

-- 插入初始任务数据
INSERT INTO `task` (`name`, `type`, `location`, `start_time`, `end_time`, `priority`, `status`, `description`) VALUES
('配送任务1', 'delivery', '39.9042, 116.4074', '2024-01-01 09:00:00', '2024-01-01 10:00:00', 'high', '待分配', '紧急配送任务'),
('巡检任务1', 'inspection', '39.9142, 116.4174', '2024-01-01 10:00:00', '2024-01-01 11:00:00', 'medium', '已分配', '电力线路巡检'),
('测绘任务1', 'survey', '39.9242, 116.4274', '2024-01-01 11:00:00', '2024-01-01 12:00:00', 'low', '已完成', '区域测绘');
