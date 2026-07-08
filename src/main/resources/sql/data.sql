-- ================================================================
-- 即刻 App 内容社区实时热点榜单引擎 - 测试数据
-- 适用于执行 schema.sql 后初始化本地演示环境。
-- ================================================================

USE jike_hotrank;

SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM topic_score_snapshot;
DELETE FROM user_circle_preference;
DELETE FROM user_behavior;
DELETE FROM interaction_event;
DELETE FROM topic;
DELETE FROM circle;
ALTER TABLE topic_score_snapshot AUTO_INCREMENT = 1;
ALTER TABLE user_circle_preference AUTO_INCREMENT = 1;
ALTER TABLE user_behavior AUTO_INCREMENT = 1;
ALTER TABLE interaction_event AUTO_INCREMENT = 1;
ALTER TABLE topic AUTO_INCREMENT = 1;
ALTER TABLE circle AUTO_INCREMENT = 1;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO circle (id, name, description, status, sort_order) VALUES
(1, '科技圈', '科技数码产品、AI 与互联网行业讨论', 1, 1),
(2, '游戏圈', '游戏攻略、版本更新与玩家心得', 1, 2),
(3, '美食圈', '探店、菜谱与城市美食分享', 1, 3),
(4, '旅行圈', '路线规划、目的地体验与出行建议', 1, 4),
(5, '读书圈', '书单推荐、读后感与知识讨论', 1, 5);

INSERT INTO topic (id, circle_id, title, content, author_id, publish_time, current_score, interaction_count, status) VALUES
(1, 1, 'ChatGPT 新功能发布', '模型更新后的体验与使用场景讨论。', 1001, DATE_SUB(NOW(), INTERVAL 1 HOUR), 0, 0, 1),
(2, 1, '小米 SU7 真实车主体验', '通勤、续航与智能座舱体验记录。', 1002, DATE_SUB(NOW(), INTERVAL 5 HOUR), 0, 0, 1),
(3, 1, '国产大模型应用盘点', '办公、编程和内容创作方向的落地情况。', 1003, DATE_SUB(NOW(), INTERVAL 40 MINUTE), 0, 0, 1),
(4, 2, '黑神话悟空通关心得', '全收集路线和 Boss 战细节复盘。', 2001, DATE_SUB(NOW(), INTERVAL 3 HOUR), 0, 0, 1),
(5, 2, '开放世界游戏的探索感', '从地图设计聊到任务密度。', 2002, DATE_SUB(NOW(), INTERVAL 7 HOUR), 0, 0, 1),
(6, 3, '北京胡同里的宝藏小馆', '人均 50 的工作日晚餐选择。', 3001, DATE_SUB(NOW(), INTERVAL 4 HOUR), 0, 0, 1),
(7, 3, '自制奶茶配方', '少糖版也能有茶香和奶香。', 3002, DATE_SUB(NOW(), INTERVAL 8 HOUR), 0, 0, 1),
(8, 4, '大理 7 天慢旅行路线', '避开人潮的洱海周边路线。', 4001, DATE_SUB(NOW(), INTERVAL 10 HOUR), 0, 0, 1),
(9, 4, '东京购物清单更新', '药妆、文具和中古店体验。', 4002, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, 0, 1),
(10, 5, '2026 年度推荐书单', '十本改变认知结构的书。', 5001, DATE_SUB(NOW(), INTERVAL 15 HOUR), 0, 0, 1),
(11, 5, '三体重读笔记', '从技术想象到文明叙事。', 5002, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, 0, 1);

INSERT INTO user_circle_preference (user_id, circle_id, weight, interaction_count) VALUES
(2001, 1, 3.50, 10),
(2001, 2, 1.50, 3),
(2002, 1, 2.00, 5),
(2002, 3, 4.00, 15),
(2003, 2, 5.00, 20);

INSERT INTO interaction_event
    (topic_id, user_id, interaction_type, device_fingerprint, ip_address, weight_multiplier, created_at)
VALUES
(1, 2001, 1, 'device_001', '192.168.1.1', 1.000, DATE_SUB(NOW(), INTERVAL 50 MINUTE)),
(1, 2002, 2, 'device_002', '192.168.1.2', 1.000, DATE_SUB(NOW(), INTERVAL 45 MINUTE)),
(1, 2003, 5, 'device_003', '192.168.1.3', 1.000, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 2004, 3, 'device_004', '192.168.1.4', 1.000, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(1, 2005, 1, 'device_005', '192.168.1.5', 1.000, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(3, 2001, 1, 'device_001', '192.168.1.1', 1.000, DATE_SUB(NOW(), INTERVAL 35 MINUTE)),
(3, 2006, 1, 'device_006', '192.168.1.6', 1.000, DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(3, 2007, 5, 'device_007', '192.168.1.7', 1.000, DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(3, 2008, 5, 'device_008', '192.168.1.8', 1.000, DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(3, 2009, 3, 'device_009', '192.168.1.9', 1.000, DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(3, 2010, 1, 'device_010', '192.168.1.10', 1.000, DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(4, 3001, 5, 'device_011', '192.168.2.1', 1.000, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(4, 3002, 1, 'device_012', '192.168.2.2', 1.000, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(4, 3003, 3, 'device_013', '192.168.2.3', 1.000, DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(6, 4001, 1, 'device_014', '192.168.3.1', 1.000, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(6, 4002, 2, 'device_015', '192.168.3.2', 1.000, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(6, 4003, 5, 'device_016', '192.168.3.3', 1.000, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(8, 5001, 3, 'device_017', '192.168.4.1', 1.000, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(10, 6001, 5, 'device_018', '192.168.5.1', 1.000, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
(1, 7001, 1, 'shared_device', '192.168.9.1', 0.500, DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(1, 7002, 1, 'shared_device', '192.168.9.2', 0.300, DATE_SUB(NOW(), INTERVAL 8 MINUTE));

INSERT INTO user_behavior
    (user_id, topic_id, interaction_type, device_fingerprint, ip_address, is_valid, invalid_reason, created_at)
SELECT user_id, topic_id, interaction_type, device_fingerprint, ip_address, 1, NULL, created_at
FROM interaction_event;

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
    t.current_score = ROUND(
        COALESCE(e.weighted_score, 0) / POW((TIMESTAMPDIFF(SECOND, t.publish_time, NOW()) / 3600) + 2, 1.8),
        4
    );
