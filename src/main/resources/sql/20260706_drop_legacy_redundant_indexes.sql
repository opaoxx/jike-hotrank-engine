-- ================================================================
-- 即刻 App 热点榜单引擎 - 旧库冗余索引清理脚本
-- 用途：清理早期课程 SQL 遗留、已被当前复合索引覆盖的冗余索引。
-- 特点：重复执行不会因为索引不存在而失败。
-- ================================================================

USE jike_hotrank;

DELIMITER //

CREATE PROCEDURE drop_index_if_exists(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` DROP INDEX `', p_index_name, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

-- topic: current schema uses status+score/status+created and circle+status+score.
CALL drop_index_if_exists('topic', 'idx_status');
CALL drop_index_if_exists('topic', 'idx_circle_score');

-- interaction_event: current schema uses wider composites with the same left prefixes.
CALL drop_index_if_exists('interaction_event', 'idx_created_at');
CALL drop_index_if_exists('interaction_event', 'idx_device');
CALL drop_index_if_exists('interaction_event', 'idx_user_topic');

-- user_behavior: current schema keeps idx_device, so only remove the single-column valid flag index.
CALL drop_index_if_exists('user_behavior', 'idx_is_valid');

DROP PROCEDURE drop_index_if_exists;

SELECT 'drop legacy redundant indexes finished' AS message;
