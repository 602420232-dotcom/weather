-- =============================================
-- 论坛模块 - 数据库初始化脚本
-- 版本: 1.0
-- 创建日期: 2026-06-10
-- =============================================

-- =============================================
-- 1. 帖子表
-- =============================================
CREATE TABLE IF NOT EXISTS forum_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(255) NOT NULL COMMENT '帖子标题',
    content TEXT NOT NULL COMMENT '帖子内容(HTML格式)',
    section VARCHAR(50) NOT NULL COMMENT '板块标识',
    status VARCHAR(20) DEFAULT 'normal' COMMENT '状态:normal/pinned/deleted',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    like_count INT DEFAULT 0 COMMENT '点赞次数',
    comment_count INT DEFAULT 0 COMMENT '评论次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_section (section) COMMENT '板块索引',
    INDEX idx_author_id (author_id) COMMENT '作者ID索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引',
    INDEX idx_status (status) COMMENT '状态索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛帖子表';

-- =============================================
-- 2. 评论表
-- =============================================
CREATE TABLE IF NOT EXISTS forum_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    parent_id BIGINT DEFAULT NULL COMMENT '父评论ID(用于回复)',
    content TEXT NOT NULL COMMENT '评论内容',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_post_id (post_id) COMMENT '帖子ID索引',
    INDEX idx_parent_id (parent_id) COMMENT '父评论ID索引',
    INDEX idx_author_id (author_id) COMMENT '作者ID索引',
    INDEX idx_created_at (created_at) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛评论表';

-- =============================================
-- 3. 帖子点赞表
-- =============================================
CREATE TABLE IF NOT EXISTS forum_post_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_post_user (post_id, user_id) COMMENT '帖子-用户唯一索引',
    INDEX idx_post_id (post_id) COMMENT '帖子ID索引',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞表';

-- =============================================
-- 4. 帖子收藏表
-- =============================================
CREATE TABLE IF NOT EXISTS forum_post_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    UNIQUE KEY uk_post_user (post_id, user_id) COMMENT '帖子-用户唯一索引',
    INDEX idx_post_id (post_id) COMMENT '帖子ID索引',
    INDEX idx_user_id (user_id) COMMENT '用户ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子收藏表';

-- =============================================
-- 5. 帖子标签关联表
-- =============================================
CREATE TABLE IF NOT EXISTS forum_post_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    post_id BIGINT NOT NULL COMMENT '帖子ID',
    tag_name VARCHAR(50) NOT NULL COMMENT '标签名称',
    INDEX idx_post_id (post_id) COMMENT '帖子ID索引',
    INDEX idx_tag_name (tag_name) COMMENT '标签名称索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子标签关联表';

-- =============================================
-- 初始化脚本完成
-- =============================================
SELECT 'Forum database initialization completed successfully' AS message;