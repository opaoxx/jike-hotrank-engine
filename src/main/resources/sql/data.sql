-- ================================================================
-- 即刻App内容社区实时热点榜单引擎 - 测试数据脚本
-- ================================================================

USE jike_hotrank;

-- ================================================================
-- 1. 插入圈子数据
-- ================================================================
INSERT INTO circle (name, description, status, sort_order) VALUES
('科技圈', '科技数码产品讨论', 1, 1),
('游戏圈', '游戏攻略与讨论', 1, 2),
('美食圈', '美食分享与探店', 1, 3),
('旅行圈', '旅行攻略与游记', 1, 4),
('读书圈', '书籍推荐与读后感', 1, 5);

-- ================================================================
-- 2. 插入话题数据
-- ================================================================
INSERT INTO topic (circle_id, title, content, author_id, publish_time, current_score, interaction_count, status) VALUES
-- 科技圈话题
(1, 'iPhone 16 Pro深度评测', '新iPhone到底值不值得买？', 1001, DATE_SUB(NOW(), INTERVAL 2 HOUR), 0, 0, 1),
(1, '小米SU7实车体验', '试驾小米汽车的真实感受', 1002, DATE_SUB(NOW(), INTERVAL 5 HOUR), 0, 0, 1),
(1, 'ChatGPT新功能发布', 'OpenAI最新更新解读', 1003, DATE_SUB(NOW(), INTERVAL 1 HOUR), 0, 0, 1),
(1, '华为Mate70曝光', '最新爆料汇总', 1004, DATE_SUB(NOW(), INTERVAL 30 MINUTE), 0, 0, 1),

-- 游戏圈话题
(2, '黑神话悟空通关心得', '历时50小时全收集通关', 2001, DATE_SUB(NOW(), INTERVAL 3 HOUR), 0, 0, 1),
(2, '原神4.0版本前瞻', '新角色新地图爆料', 2002, DATE_SUB(NOW(), INTERVAL 6 HOUR), 0, 0, 1),
(2, '王者荣耀新赛季上分攻略', 'S35赛季推荐英雄', 2003, DATE_SUB(NOW(), INTERVAL 12 HOUR), 0, 0, 1),

-- 美食圈话题
(3, '北京探店｜隐藏在胡同里的宝藏小馆', '人均50吃到撑', 3001, DATE_SUB(NOW(), INTERVAL 4 HOUR), 0, 0, 1),
(3, '自制奶茶教程', '比外面卖的还好喝', 3002, DATE_SUB(NOW(), INTERVAL 8 HOUR), 0, 0, 1),
(3, '上海米其林餐厅打卡', '值得排队的店', 3003, DATE_SUB(NOW(), INTERVAL 1 HOUR), 0, 0, 1),

-- 旅行圈话题
(4, '云南大理7天深度游攻略', '避开人潮的小众路线', 4001, DATE_SUB(NOW(), INTERVAL 10 HOUR), 0, 0, 1),
(4, '日本东京购物指南', '药妆店必买清单', 4002, DATE_SUB(NOW(), INTERVAL 24 HOUR), 0, 0, 1),

-- 读书圈话题
(5, '2024年度推荐书单', '这10本书改变了我的认知', 5001, DATE_SUB(NOW(), INTERVAL 15 HOUR), 0, 0, 1),
(5, '三体读后感', '中国科幻的巅峰之作', 5002, DATE_SUB(NOW(), INTERVAL 48 HOUR), 0, 0, 1);

-- ================================================================
-- 3. 插入用户圈子偏好数据（模拟用户历史互动）
-- ================================================================
INSERT INTO user_circle_preference (user_id, circle_id, weight, interaction_count) VALUES
(2001, 1, 3.50, 10),  -- 用户2001对科技圈偏好较高
(2001, 2, 1.50, 3),   -- 用户2001对游戏圈偏好一般
(2002, 1, 2.00, 5),   -- 用户2002对科技圈偏好中等
(2002, 3, 4.00, 15),  -- 用户2002对美食圈偏好较高
(2003, 2, 5.00, 20);  -- 用户2003对游戏圈偏好很高

-- ================================================================
-- 4. 插入互动事件数据
-- ================================================================
-- 为科技圈话题1（iPhone 16 Pro）添加互动
INSERT INTO interaction_event (topic_id, user_id, interaction_type, device_fingerprint, ip_address, created_at) VALUES
(1, 2001, 1, 'device_001', '192.168.1.1', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 2002, 1, 'device_002', '192.168.1.2', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(1, 2003, 5, 'device_003', '192.168.1.3', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(1, 2004, 3, 'device_004', '192.168.1.4', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(1, 2005, 2, 'device_005', '192.168.1.5', DATE_SUB(NOW(), INTERVAL 15 MINUTE)),

-- 为科技圈话题3（ChatGPT）添加更多互动
(3, 2001, 1, 'device_001', '192.168.1.1', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(3, 2006, 1, 'device_006', '192.168.1.6', DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(3, 2007, 5, 'device_007', '192.168.1.7', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(3, 2008, 5, 'device_008', '192.168.1.8', DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(3, 2009, 3, 'device_009', '192.168.1.9', DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(3, 2010, 1, 'device_010', '192.168.1.10', DATE_SUB(NOW(), INTERVAL 5 MINUTE)),

-- 为科技圈话题4（华为Mate70）添加互动（新话题，互动多）
(4, 2001, 1, 'device_001', '192.168.1.1', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
(4, 2002, 5, 'device_002', '192.168.1.2', DATE_SUB(NOW(), INTERVAL 18 MINUTE)),
(4, 2003, 3, 'device_003', '192.168.1.3', DATE_SUB(NOW(), INTERVAL 15 MINUTE)),
(4, 2004, 1, 'device_004', '192.168.1.4', DATE_SUB(NOW(), INTERVAL 12 MINUTE)),
(4, 2005, 1, 'device_005', '192.168.1.5', DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
(4, 2006, 2, 'device_006', '192.168.1.6', DATE_SUB(NOW(), INTERVAL 8 MINUTE)),
(4, 2007, 5, 'device_007', '192.168.1.7', DATE_SUB(NOW(), INTERVAL 5 MINUTE)),
(4, 2008, 3, 'device_008', '192.168.1.8', DATE_SUB(NOW(), INTERVAL 3 MINUTE)),

-- 为游戏圈话题1（黑神话悟空）添加互动
(5, 3001, 5, 'device_011', '192.168.2.1', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(5, 3002, 1, 'device_012', '192.168.2.2', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(5, 3003, 3, 'device_013', '192.168.2.3', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),

-- 为美食圈话题1（北京探店）添加互动
(8, 4001, 1, 'device_014', '192.168.3.1', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(8, 4002, 2, 'device_015', '192.168.3.2', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(8, 4003, 5, 'device_016', '192.168.3.3', DATE_SUB(NOW(), INTERVAL 1 HOUR));
