-- =============================================
-- 论坛模块表结构
-- =============================================

-- 创建用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
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

-- 插入初始用户数据
INSERT INTO `sys_user` (`id`, `username`, `password`, `role`, `name`, `email`, `phone`) VALUES
(1, 'admin', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'admin', '管理员', 'admin@example.com', '13800138000'),
(2, 'dispatcher', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'dispatcher', '调度员', 'dispatcher@example.com', '13800138001'),
(3, 'operator', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'operator', '操作员', 'operator@example.com', '13800138002'),
(4, 'user', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW', 'user', '普通用户', 'user@example.com', '13800138003')
ON DUPLICATE KEY UPDATE username=username;

-- 插入初始论坛数据
INSERT INTO `forum_posts` (`title`, `content`, `section`, `status`, `author_id`, `view_count`, `like_count`, `comment_count`, `created_at`) VALUES
('V2.0版本发布公告', '<p>各位同事，经过两周的开发和测试，我们的无人机路径规划系统V2.0版本正式发布！</p><p><strong>新增功能：</strong></p><ul><li>支持VRPTW路径规划算法</li><li>集成实时气象数据</li><li>新增多语言支持</li><li>优化界面响应速度</li></ul>', 'announcement', 'pinned', 1, 128, 24, 2, '2026-06-08 10:00:00'),
('关于DE-RRT*算法参数调优的讨论', '<p>最近在测试DE-RRT*算法时发现，当障碍物密度较高时，路径规划时间明显增加。欢迎大家分享优化经验。</p>', 'tech_discuss', 'normal', 2, 86, 15, 1, '2026-06-07 14:30:00'),
('飞控端GPS信号异常问题反馈', '<p>今天在现场测试时发现，无人机在飞行过程中出现GPS信号丢失的情况，导致自动返航功能触发。</p>', 'task_collab', 'normal', 3, 45, 8, 1, '2026-06-07 16:45:00'),
('系统部署指南（完整版）', '<h3>1. 环境要求</h3><p>操作系统：Ubuntu 20.04 LTS</p><p>内存：至少8GB</p>', 'knowledge', 'pinned', 4, 203, 32, 1, '2026-06-05 09:00:00')
ON DUPLICATE KEY UPDATE title=title;

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
