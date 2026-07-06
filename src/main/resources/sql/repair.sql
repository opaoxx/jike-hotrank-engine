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
WHERE weight_multiplier IS NULL;

-- 3. 按当前算法重算话题互动数与热度分。
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
            END * COALESCE(weight_multiplier, 1.000)) AS weighted_score
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

-- 4. 重建用户圈子偏好，保持与个性化榜单逻辑一致。
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

-- 5. 清理过期快照，保留最近 7 天。
DELETE FROM topic_score_snapshot
WHERE snapshot_time < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- 6. 验证修复结果：返回仍不一致的话题。
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
