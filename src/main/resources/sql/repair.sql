-- ================================================================
-- 即刻 App 热点榜单引擎 - 数据修复脚本
-- 用途：修复历史数据中的互动计数、热度分、偏好权重与孤立记录。
-- ================================================================

USE jike_hotrank;

-- 1. 清理孤立记录，避免后续统计引用不存在的话题。
DELETE ie FROM interaction_event ie
LEFT JOIN topic t ON ie.topic_id = t.id
WHERE t.id IS NULL;

DELETE ub FROM user_behavior ub
LEFT JOIN topic t ON ub.topic_id = t.id
WHERE t.id IS NULL;

DELETE s FROM topic_score_snapshot s
LEFT JOIN topic t ON s.topic_id = t.id
WHERE t.id IS NULL;

-- 2. 补齐旧数据的防刷权重倍率。
UPDATE interaction_event
SET weight_multiplier = 1.000
WHERE weight_multiplier <= 0;

-- 3. 回填旧互动事件缺失的有效行为审计。
INSERT INTO user_behavior
    (user_id, topic_id, interaction_type, device_fingerprint, ip_address, is_valid, invalid_reason, created_at)
SELECT
    ie.user_id,
    ie.topic_id,
    ie.interaction_type,
    ie.device_fingerprint,
    ie.ip_address,
    1,
    NULL,
    ie.created_at
FROM interaction_event ie
WHERE NOT EXISTS (
    SELECT 1
    FROM user_behavior ub
    WHERE ub.user_id = ie.user_id
      AND ub.topic_id = ie.topic_id
      AND ub.interaction_type = ie.interaction_type
      AND ub.created_at = ie.created_at
      AND ub.device_fingerprint <=> ie.device_fingerprint
      AND ub.ip_address <=> ie.ip_address
);

-- 4. 按当前算法重算话题互动数与热度分。
-- noinspection SqlWithoutWhere
UPDATE topic t
LEFT JOIN (
    SELECT
        topic_id,
        COUNT(*) AS total_count,
        SUM(CASE interaction_type
                WHEN 1 THEN 1
                WHEN 2 THEN 2
                WHEN 3 THEN 3
                WHEN 5 THEN 5
                ELSE 0
            END * weight_multiplier) AS weighted_score
    FROM interaction_event
    GROUP BY topic_id
) e ON e.topic_id = t.id
SET t.interaction_count = COALESCE(e.total_count, 0),
    t.current_score = CASE
        WHEN t.status = 0 THEN 0
        ELSE ROUND(
            COALESCE(e.weighted_score, 0) / POW((TIMESTAMPDIFF(SECOND, t.publish_time, NOW()) / 3600) + 2, 1.8),
            4
        )
    END;

-- 5. 重建用户圈子偏好，保持与个性化榜单逻辑一致。
-- noinspection SqlWithoutWhere
DELETE FROM user_circle_preference;

INSERT INTO user_circle_preference (user_id, circle_id, weight, interaction_count)
SELECT
    ie.user_id,
    t.circle_id,
    LEAST(1 + LOG2(COUNT(*)), 10.00) AS weight,
    COUNT(*) AS interaction_count
FROM interaction_event ie
JOIN topic t ON ie.topic_id = t.id
GROUP BY ie.user_id, t.circle_id;

-- 6. 清理过期快照，保留最近 7 天。
DELETE FROM topic_score_snapshot
WHERE snapshot_time < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- 7. 验证修复结果：返回仍不一致的话题。
SELECT
    t.id,
    t.title,
    t.interaction_count AS recorded_count,
    COUNT(ie.id) AS actual_count,
    CASE
        WHEN t.interaction_count = COUNT(ie.id) THEN 'OK'
        ELSE 'MISMATCH'
    END AS check_status
FROM topic t
LEFT JOIN interaction_event ie ON t.id = ie.topic_id
GROUP BY t.id, t.title, t.interaction_count
HAVING t.interaction_count != COUNT(ie.id);

SELECT 'repair finished' AS message;
