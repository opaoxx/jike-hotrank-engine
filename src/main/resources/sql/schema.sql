CREATE DATABASE IF NOT EXISTS jike_hotrank
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE jike_hotrank;

DROP TABLE IF EXISTS circle;
CREATE TABLE circle (
    id              BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT 'circle id',
    name            VARCHAR(100) NOT NULL COMMENT 'circle name',
    description     VARCHAR(500) DEFAULT NULL COMMENT 'circle description',
    icon_url        VARCHAR(255) DEFAULT NULL COMMENT 'circle icon url',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '0-disabled, 1-enabled',
    sort_order      INT          NOT NULL DEFAULT 0 COMMENT 'sort order',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    UNIQUE KEY uk_name (name),
    INDEX idx_status_sort (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='circle';

DROP TABLE IF EXISTS topic;
CREATE TABLE topic (
    id                BIGINT         PRIMARY KEY AUTO_INCREMENT COMMENT 'topic id',
    circle_id         BIGINT         NOT NULL COMMENT 'circle id',
    title             VARCHAR(200)   NOT NULL COMMENT 'topic title',
    content           TEXT           COMMENT 'topic content',
    author_id         BIGINT         NOT NULL COMMENT 'author id',
    publish_time      DATETIME       NOT NULL COMMENT 'publish time',
    current_score     DECIMAL(20, 4) NOT NULL DEFAULT 0 COMMENT 'current heat score',
    interaction_count INT            NOT NULL DEFAULT 0 COMMENT 'interaction count',
    status            TINYINT        NOT NULL DEFAULT 1 COMMENT '0-blocked, 1-normal, 2-review',
    created_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at        DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    INDEX idx_status_score (status, current_score DESC),
    INDEX idx_circle_status_score (circle_id, status, current_score DESC),
    INDEX idx_newcomer_rank (status, publish_time, current_score DESC),
    INDEX idx_publish_time (publish_time),
    INDEX idx_author (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='topic';

DROP TABLE IF EXISTS interaction_event;
CREATE TABLE interaction_event (
    id                  BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT 'event id',
    topic_id            BIGINT        NOT NULL COMMENT 'topic id',
    user_id             BIGINT        NOT NULL COMMENT 'user id',
    interaction_type    TINYINT       NOT NULL COMMENT '1-like, 2-bookmark, 3-share, 5-comment',
    device_fingerprint  VARCHAR(100)  DEFAULT NULL COMMENT 'device fingerprint',
    ip_address          VARCHAR(50)   DEFAULT NULL COMMENT 'ip address',
    weight_multiplier   DECIMAL(6, 3) NOT NULL DEFAULT 1.000 COMMENT 'anti-spam heat multiplier',
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',

    INDEX idx_topic_created (topic_id, created_at),
    INDEX idx_user_topic_created (user_id, topic_id, created_at),
    INDEX idx_created_topic_type (created_at, topic_id, interaction_type),
    INDEX idx_device_created_user (device_fingerprint, created_at, user_id),
    INDEX idx_user_device_created (user_id, device_fingerprint, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='interaction event';

DROP TABLE IF EXISTS user_behavior;
CREATE TABLE user_behavior (
    id                  BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT 'behavior id',
    user_id             BIGINT       NOT NULL COMMENT 'user id',
    topic_id            BIGINT       NOT NULL COMMENT 'topic id',
    interaction_type    TINYINT      NOT NULL COMMENT 'interaction type',
    device_fingerprint  VARCHAR(100) DEFAULT NULL COMMENT 'device fingerprint',
    ip_address          VARCHAR(50)  DEFAULT NULL COMMENT 'ip address',
    is_valid            TINYINT      NOT NULL DEFAULT 1 COMMENT '0-invalid, 1-valid',
    invalid_reason      VARCHAR(200) DEFAULT NULL COMMENT 'invalid reason',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',

    INDEX idx_user_created (user_id, created_at),
    INDEX idx_topic_created (topic_id, created_at),
    INDEX idx_device (device_fingerprint),
    INDEX idx_is_valid (is_valid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user behavior';

DROP TABLE IF EXISTS topic_score_snapshot;
CREATE TABLE topic_score_snapshot (
    id              BIGINT         PRIMARY KEY AUTO_INCREMENT COMMENT 'snapshot id',
    topic_id        BIGINT         NOT NULL COMMENT 'topic id',
    circle_id       BIGINT         NOT NULL COMMENT 'circle id',
    score           DECIMAL(20, 4) NOT NULL COMMENT 'heat score',
    rank_position   INT            NOT NULL COMMENT 'rank position',
    snapshot_time   DATETIME       NOT NULL COMMENT 'snapshot time',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',

    INDEX idx_snapshot_time (snapshot_time),
    INDEX idx_topic_snapshot (topic_id, snapshot_time),
    INDEX idx_circle_snapshot (circle_id, snapshot_time, rank_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='topic score snapshot';

DROP TABLE IF EXISTS user_circle_preference;
CREATE TABLE user_circle_preference (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT 'preference id',
    user_id           BIGINT        NOT NULL COMMENT 'user id',
    circle_id         BIGINT        NOT NULL COMMENT 'circle id',
    weight            DECIMAL(5, 2) NOT NULL DEFAULT 1.00 COMMENT 'preference weight',
    interaction_count INT           NOT NULL DEFAULT 0 COMMENT 'interaction count',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'created time',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'updated time',

    UNIQUE KEY uk_user_circle (user_id, circle_id),
    INDEX idx_user_weight (user_id, weight DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user circle preference';
