-- ================================================================
-- 即刻App热点榜单引擎 - 数据修复脚本
-- 用途：修复历史已产生的不一致数据
-- ================================================================

USE jike_hotrank;

-- ================================================================
-- 1. 修复话题热度分与实际互动数据不一致
-- ================================================================
-- 重新计算所有话题的互动次数（基于interaction_event表实际数据）
UPDATE topic t
SET interaction_count = (
    SELECT COUNT(*)
    FROM interaction_event ie
    WHERE ie.topic_id = t.id
)
WHERE t.status = 1;

-- ================================================================
-- 2. 修复用户圈子偏好数据
-- ================================================================
-- 重新统计用户圈子互动次数
INSERT INTO user_circle_preference (user_id, circle_id, weight, interaction_count)
SELECT
    ie.user_id,
    t.circle_id,
    1.00 AS weight,
    COUNT(*) AS interaction_count
FROM interaction_event ie
JOIN topic t ON ie.topic_id = t.id
GROUP BY ie.user_id, t.circle_id
ON DUPLICATE KEY UPDATE
    interaction_count = (
        SELECT COUNT(*)
        FROM interaction_event ie2
        JOIN topic t2 ON ie2.topic_id = t2.id
        WHERE ie2.user_id = user_circle_preference.user_id
          AND t2.circle_id = user_circle_preference.circle_id
    );

-- 更新偏好权重（基于互动次数）
UPDATE user_circle_preference
SET weight = LEAST(1 + LOG2(interaction_count), 10.00)
WHERE interaction_count > 0;

-- ================================================================
-- 3. 清理孤立的互动事件（话题不存在）
-- ================================================================
DELETE ie FROM interaction_event ie
LEFT JOIN topic t ON ie.topic_id = t.id
WHERE t.id IS NULL;

-- ================================================================
-- 4. 清理孤立的用户行为记录（话题不存在）
-- ================================================================
DELETE ub FROM user_behavior ub
LEFT JOIN topic t ON ub.topic_id = t.id
WHERE t.id IS NULL;

-- ================================================================
-- 5. 清理过期的快照数据（保留最近7天）
-- ================================================================
DELETE FROM topic_score_snapshot
WHERE snapshot_time < DATE_SUB(NOW(), INTERVAL 7 DAY);

-- ================================================================
-- 6. 重置被屏蔽话题的热度分为0
-- ================================================================
UPDATE topic
SET current_score = 0
WHERE status = 0;

-- ================================================================
-- 7. 验证数据一致性
-- ================================================================
-- 检查话题互动次数是否正确
SELECT
    t.id,
    t.title,
    t.interaction_count AS recorded_count,
    COUNT(ie.id) AS actual_count,
    CASE
        WHEN t.interaction_count = COUNT(ie.id) THEN 'OK'
        ELSE 'MISMATCH'
    END AS status
FROM topic t
LEFT JOIN interaction_event ie ON t.id = ie.topic_id
WHERE t.status = 1
GROUP BY t.id, t.title, t.interaction_count
HAVING t.interaction_count != COUNT(ie.id);

-- 完成提示
SELECT '数据修复完成' AS message;
