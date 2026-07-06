# 数据库设计文档

## 概览

- 数据库名：`jike_hotrank`
- 字符集：`utf8mb4`
- 排序规则：`utf8mb4_unicode_ci`
- 存储引擎：`InnoDB`

## 核心表

### circle

圈子主数据表，保存圈子名称、描述、状态和排序权重。

关键索引：

- `uk_name (name)`：保证圈子名唯一。
- `idx_status_sort (status, sort_order)`：支持启用圈子按排序权重展示。

### topic

话题表，保存圈子、作者、发布时间、当前热度分、互动数和状态。

关键索引：

- `idx_status_score (status, current_score DESC)`：全站热榜。
- `idx_circle_status_score (circle_id, status, current_score DESC)`：圈子热榜。
- `idx_newcomer_rank (status, publish_time, current_score DESC)`：24 小时新星榜。
- `idx_publish_time (publish_time)`：发布时间范围检索。
- `idx_author (author_id)`：作者维度检索。

### interaction_event

互动事件流水表，保存点赞、收藏、转发、评论以及反作弊热度倍率。

字段 `weight_multiplier` 用于记录反作弊降权后的热度贡献倍率，默认 `1.000`。

关键索引：

- `idx_topic_created (topic_id, created_at)`：按话题统计时间窗口内互动数。
- `idx_user_topic_created (user_id, topic_id, created_at)`：用户对同一话题的频率限制。
- `idx_created_topic_type (created_at, topic_id, interaction_type)`：热度聚合与飙升榜时间窗口统计。
- `idx_device_created_user (device_fingerprint, created_at, user_id)`：设备指纹关联用户检测。
- `idx_user_device_created (user_id, device_fingerprint, created_at)`：判断用户是否已使用过设备。

### user_behavior

用户行为审计表，保存有效与无效行为。无效行为不会进入 `interaction_event`，但会保留原因，供反作弊报告使用。

关键索引：

- `idx_user_created (user_id, created_at)`
- `idx_topic_created (topic_id, created_at)`
- `idx_device (device_fingerprint)`
- `idx_is_valid (is_valid)`

### topic_score_snapshot

榜单快照表，用于历史排名与趋势分析。

关键索引：

- `idx_snapshot_time (snapshot_time)`
- `idx_topic_snapshot (topic_id, snapshot_time)`
- `idx_circle_snapshot (circle_id, snapshot_time, rank_position)`

### user_circle_preference

用户圈子偏好表，用于个性化榜单重排。

关键索引：

- `uk_user_circle (user_id, circle_id)`：每个用户对每个圈子只有一条偏好。
- `idx_user_weight (user_id, weight DESC)`：读取用户偏好时按权重排序。

## 主要查询路径

全站热榜：

```sql
SELECT *
FROM topic
WHERE status = 1
ORDER BY current_score DESC
LIMIT ?;
```

圈子热榜：

```sql
SELECT *
FROM topic
WHERE circle_id = ?
  AND status = 1
ORDER BY current_score DESC
LIMIT ?;
```

互动聚合：

```sql
SELECT topic_id, interaction_type, COUNT(*)
FROM interaction_event
WHERE created_at >= ?
  AND created_at < ?
GROUP BY topic_id, interaction_type;
```

设备指纹检测：

```sql
SELECT COUNT(DISTINCT user_id)
FROM interaction_event
WHERE device_fingerprint = ?
  AND created_at >= ?;
```

## 维护脚本

- `schema.sql`：新库初始化。
- `data.sql`：本地演示数据初始化。
- `20260706_add_interaction_weight_multiplier.sql`：给旧库补反作弊热度倍率字段。
- `20260706_add_rank_query_indexes.sql`：给旧库补榜单、聚合和反作弊索引。
- `repair.sql`：清理孤立数据，并按当前算法重算互动数、热度分和用户圈子偏好。
