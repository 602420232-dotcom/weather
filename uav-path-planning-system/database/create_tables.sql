-- 创建业务数据库并授权
CREATE DATABASE IF NOT EXISTS `wrf_processor`;
CREATE DATABASE IF NOT EXISTS `data_assimilation`;
CREATE DATABASE IF NOT EXISTS `meteor_forecast`;
CREATE DATABASE IF NOT EXISTS `path_planning`;
CREATE DATABASE IF NOT EXISTS `uav_weather`;
GRANT ALL PRIVILEGES ON `wrf_processor`.* TO 'uav'@'%';
GRANT ALL PRIVILEGES ON `data_assimilation`.* TO 'uav'@'%';
GRANT ALL PRIVILEGES ON `meteor_forecast`.* TO 'uav'@'%';
GRANT ALL PRIVILEGES ON `path_planning`.* TO 'uav'@'%';
GRANT ALL PRIVILEGES ON `uav_weather`.* TO 'uav'@'%';
FLUSH PRIVILEGES;

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

-- =============================================
-- 论坛模块表结构
-- =============================================

-- 创建帖子表
CREATE TABLE IF NOT EXISTS `forum_posts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `section` VARCHAR(50) NOT NULL,
    `status` VARCHAR(20) DEFAULT 'normal',
    `author_id` BIGINT NOT NULL,
    `view_count` INT DEFAULT 0,
    `like_count` INT DEFAULT 0,
    `comment_count` INT DEFAULT 0,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_section` (`section`),
    INDEX `idx_author_id` (`author_id`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建评论表
CREATE TABLE IF NOT EXISTS `forum_comments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT NULL,
    `content` TEXT NOT NULL,
    `author_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_post_id` (`post_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建帖子点赞表
CREATE TABLE IF NOT EXISTS `forum_post_likes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user_like` (`post_id`, `user_id`),
    INDEX `idx_post_id_like` (`post_id`),
    INDEX `idx_user_id_like` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建帖子收藏表
CREATE TABLE IF NOT EXISTS `forum_post_favorites` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user_favorite` (`post_id`, `user_id`),
    INDEX `idx_post_id_favorite` (`post_id`),
    INDEX `idx_user_id_favorite` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建帖子标签表
CREATE TABLE IF NOT EXISTS `forum_post_tags` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `post_id` BIGINT NOT NULL,
    `tag_name` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_post_id_tag` (`post_id`),
    INDEX `idx_tag_name` (`tag_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入初始论坛数据
INSERT INTO `forum_posts` (`title`, `content`, `section`, `status`, `author_id`, `view_count`, `like_count`, `comment_count`, `created_at`) VALUES
('V2.0版本发布公告', '<p>各位同事，经过两周的开发和测试，我们的无人机路径规划系统V2.0版本正式发布！</p><p><strong>新增功能：</strong></p><ul><li>支持VRPTW路径规划算法</li><li>集成实时气象数据</li><li>新增多语言支持</li><li>优化界面响应速度</li></ul>', 'announcement', 'pinned', 1, 128, 24, 2, '2026-06-08 10:00:00'),
('关于DE-RRT*算法参数调优的讨论', '<p>最近在测试DE-RRT*算法时发现，当障碍物密度较高时，路径规划时间明显增加。欢迎大家分享优化经验。</p>', 'tech_discuss', 'normal', 2, 86, 15, 1, '2026-06-07 14:30:00'),
('飞控端GPS信号异常问题反馈', '<p>今天在现场测试时发现，无人机在飞行过程中出现GPS信号丢失的情况，导致自动返航功能触发。</p>', 'task_collab', 'normal', 3, 45, 8, 1, '2026-06-07 16:45:00'),
('系统部署指南（完整版）', '<h3>1. 环境要求</h3><p>操作系统：Ubuntu 20.04 LTS</p><p>内存：至少8GB</p>', 'knowledge', 'pinned', 4, 203, 32, 1, '2026-06-05 09:00:00');

-- 插入初始评论数据
INSERT INTO `forum_comments` (`post_id`, `content`, `author_id`, `created_at`) VALUES
(1, '收到，我们测试部门会尽快安排测试！', 2, '2026-06-08 10:30:00'),
(1, '部署这边已经准备就绪，等待测试通过后进行部署。', 3, '2026-06-08 11:00:00'),
(2, '试试调整启发式函数的权重，可能会有帮助。', 4, '2026-06-07 15:20:00'),
(3, '我来分析一下，可能是信号干扰问题。', 2, '2026-06-07 17:00:00'),
(4, '文档很详细，谢谢分享！', 3, '2026-06-05 14:00:00');

-- 插入初始标签数据
INSERT INTO `forum_post_tags` (`post_id`, `tag_name`) VALUES
(1, '版本发布'), (1, '系统更新'),
(2, '算法'), (2, 'DE-RRT*'), (2, '参数调优'),
(3, '问题反馈'), (3, 'GPS'), (3, '飞控'),
(4, '部署'), (4, '文档'), (4, '指南');
