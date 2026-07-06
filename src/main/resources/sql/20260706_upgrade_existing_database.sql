-- ================================================================
-- 即刻 App 热点榜单引擎 - 旧库幂等升级脚本
-- 用途：已有旧表时补齐当前项目所需字段、表和索引。
-- 特点：重复执行不会因为字段或索引已存在而失败。
-- ================================================================

CREATE DATABASE IF NOT EXISTS jike_hotrank
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE jike_hotrank;

CREATE TABLE IF NOT EXISTS user_circle_preference (
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

DELIMITER //

CREATE PROCEDURE add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_alter_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND column_name = p_column_name
    ) THEN
        SET @ddl = p_alter_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CREATE PROCEDURE add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_alter_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = p_alter_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL add_column_if_missing(
    'interaction_event',
    'weight_multiplier',
    'ALTER TABLE interaction_event ADD COLUMN weight_multiplier DECIMAL(6, 3) NOT NULL DEFAULT 1.000 COMMENT ''anti-spam heat multiplier'' AFTER ip_address'
);

CALL add_index_if_missing('topic', 'idx_status_score',
    'ALTER TABLE topic ADD INDEX idx_status_score (status, current_score DESC)');
CALL add_index_if_missing('topic', 'idx_circle_status_score',
    'ALTER TABLE topic ADD INDEX idx_circle_status_score (circle_id, status, current_score DESC)');
CALL add_index_if_missing('topic', 'idx_newcomer_rank',
    'ALTER TABLE topic ADD INDEX idx_newcomer_rank (status, publish_time, current_score DESC)');
CALL add_index_if_missing('topic', 'idx_status_created',
    'ALTER TABLE topic ADD INDEX idx_status_created (status, created_at DESC)');

CALL add_index_if_missing('interaction_event', 'idx_user_topic_created',
    'ALTER TABLE interaction_event ADD INDEX idx_user_topic_created (user_id, topic_id, created_at)');
CALL add_index_if_missing('interaction_event', 'idx_created_topic_type',
    'ALTER TABLE interaction_event ADD INDEX idx_created_topic_type (created_at, topic_id, interaction_type)');
CALL add_index_if_missing('interaction_event', 'idx_created_type',
    'ALTER TABLE interaction_event ADD INDEX idx_created_type (created_at, interaction_type)');
CALL add_index_if_missing('interaction_event', 'idx_device_created_user',
    'ALTER TABLE interaction_event ADD INDEX idx_device_created_user (device_fingerprint, created_at, user_id)');
CALL add_index_if_missing('interaction_event', 'idx_user_device_created',
    'ALTER TABLE interaction_event ADD INDEX idx_user_device_created (user_id, device_fingerprint, created_at)');

CALL add_index_if_missing('user_behavior', 'idx_user_topic_valid_created',
    'ALTER TABLE user_behavior ADD INDEX idx_user_topic_valid_created (user_id, topic_id, is_valid, created_at)');
CALL add_index_if_missing('user_behavior', 'idx_device',
    'ALTER TABLE user_behavior ADD INDEX idx_device (device_fingerprint)');
CALL add_index_if_missing('user_behavior', 'idx_behavior_device_created_user',
    'ALTER TABLE user_behavior ADD INDEX idx_behavior_device_created_user (device_fingerprint, created_at, user_id)');
CALL add_index_if_missing('user_behavior', 'idx_valid_created_topic',
    'ALTER TABLE user_behavior ADD INDEX idx_valid_created_topic (is_valid, created_at DESC, topic_id)');

DROP PROCEDURE add_column_if_missing;
DROP PROCEDURE add_index_if_missing;

SELECT 'upgrade existing database finished' AS message;
