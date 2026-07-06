-- ================================================================
-- 即刻App内容社区实时热点榜单引擎 - 数据库建表脚本
-- 数据库：MySQL 8.0
-- 字符集：utf8mb4
-- ================================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS jike_hotrank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE jike_hotrank;

-- ================================================================
-- 1. 圈子表 (circle)
-- 描述：兴趣圈子，如科技圈、游戏圈、美食圈等
-- ================================================================
DROP TABLE IF EXISTS circle;
CREATE TABLE circle (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '圈子ID',
    name            VARCHAR(100)    NOT NULL                    COMMENT '圈子名称',
    description     VARCHAR(500)    DEFAULT NULL                COMMENT '圈子描述',
    icon_url        VARCHAR(255)    DEFAULT NULL                COMMENT '圈子图标URL',
    status          TINYINT         NOT NULL DEFAULT 1          COMMENT '状态：0-禁用 1-启用',
    sort_order      INT             NOT NULL DEFAULT 0          COMMENT '排序权重',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_name (name),
    INDEX idx_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='圈子表';

-- ================================================================
-- 2. 话题表 (topic)
-- 描述：用户发布的话题内容，是热度计算的核心主体
-- ================================================================
DROP TABLE IF EXISTS topic;
CREATE TABLE topic (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '话题ID',
    circle_id       BIGINT          NOT NULL                    COMMENT '所属圈子ID',
    title           VARCHAR(200)    NOT NULL                    COMMENT '话题标题',
    content         TEXT                                        COMMENT '话题内容',
    author_id       BIGINT          NOT NULL                    COMMENT '作者用户ID',
    publish_time    DATETIME        NOT NULL                    COMMENT '发布时间',
    current_score   DECIMAL(20, 4)  NOT NULL DEFAULT 0          COMMENT '当前热度分',
    interaction_count INT           NOT NULL DEFAULT 0          COMMENT '总互动次数',
    status          TINYINT         NOT NULL DEFAULT 1          COMMENT '状态：0-屏蔽 1-正常 2-待审核',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_circle_score (circle_id, current_score DESC),
    INDEX idx_publish_time (publish_time),
    INDEX idx_status (status),
    INDEX idx_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题表';

-- ================================================================
-- 3. 互动事件表 (interaction_event)
-- 描述：用户的每次互动行为记录（点赞/收藏/转发/评论）
-- ================================================================
DROP TABLE IF EXISTS interaction_event;
CREATE TABLE interaction_event (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '事件ID',
    topic_id            BIGINT          NOT NULL                    COMMENT '话题ID',
    user_id             BIGINT          NOT NULL                    COMMENT '用户ID',
    interaction_type    TINYINT         NOT NULL                    COMMENT '互动类型：1-点赞 2-收藏 3-转发 5-评论',
    device_fingerprint  VARCHAR(100)    DEFAULT NULL                COMMENT '设备指纹',
    ip_address          VARCHAR(50)     DEFAULT NULL                COMMENT 'IP地址',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',

    INDEX idx_topic_created (topic_id, created_at),
    INDEX idx_user_topic (user_id, topic_id),
    INDEX idx_created_at (created_at),
    INDEX idx_device (device_fingerprint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='互动事件表';

-- ================================================================
-- 4. 用户行为表 (user_behavior)
-- 描述：用户行为记录，用于防刷检测
-- ================================================================
DROP TABLE IF EXISTS user_behavior;
CREATE TABLE user_behavior (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '行为ID',
    user_id             BIGINT          NOT NULL                    COMMENT '用户ID',
    topic_id            BIGINT          NOT NULL                    COMMENT '话题ID',
    interaction_type    TINYINT         NOT NULL                    COMMENT '互动类型',
    device_fingerprint  VARCHAR(100)    DEFAULT NULL                COMMENT '设备指纹',
    ip_address          VARCHAR(50)     DEFAULT NULL                COMMENT 'IP地址',
    is_valid            TINYINT         NOT NULL DEFAULT 1          COMMENT '是否有效：0-无效(被拦截) 1-有效',
    invalid_reason      VARCHAR(200)    DEFAULT NULL                COMMENT '无效原因',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',

    INDEX idx_user_created (user_id, created_at),
    INDEX idx_topic_created (topic_id, created_at),
    INDEX idx_device (device_fingerprint),
    INDEX idx_is_valid (is_valid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户行为表';

-- ================================================================
-- 5. 话题热度快照表 (topic_score_snapshot)
-- 描述：每小时记录一次TOP100榜单快照，支持历史回溯
-- ================================================================
DROP TABLE IF EXISTS topic_score_snapshot;
CREATE TABLE topic_score_snapshot (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '快照ID',
    topic_id        BIGINT          NOT NULL                    COMMENT '话题ID',
    circle_id       BIGINT          NOT NULL                    COMMENT '圈子ID',
    score           DECIMAL(20, 4)  NOT NULL                    COMMENT '热度分',
    rank_position   INT             NOT NULL                    COMMENT '排名位置',
    snapshot_time   DATETIME        NOT NULL                    COMMENT '快照时间',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',

    INDEX idx_snapshot_time (snapshot_time),
    INDEX idx_topic_snapshot (topic_id, snapshot_time),
    INDEX idx_circle_snapshot (circle_id, snapshot_time, rank_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='话题热度快照表';

-- ================================================================
-- 6. 用户圈子偏好表 (user_circle_preference) - 阶段四新增
-- 描述：用户对不同圈子的偏好权重，用于个性化榜单
-- ================================================================
DROP TABLE IF EXISTS user_circle_preference;
CREATE TABLE user_circle_preference (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT  COMMENT '偏好ID',
    user_id         BIGINT          NOT NULL                    COMMENT '用户ID',
    circle_id       BIGINT          NOT NULL                    COMMENT '圈子ID',
    weight          DECIMAL(5, 2)   NOT NULL DEFAULT 1.00       COMMENT '偏好权重（0.01-10.00）',
    interaction_count INT           NOT NULL DEFAULT 0          COMMENT '互动次数',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_user_circle (user_id, circle_id),
    INDEX idx_user_weight (user_id, weight DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户圈子偏好表';
