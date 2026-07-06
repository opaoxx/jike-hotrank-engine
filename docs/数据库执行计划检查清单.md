# SQL 执行计划检查清单

本清单用于 Day5 性能审查。执行前先初始化数据，并确保旧库已经执行所有迁移脚本。

## 榜单查询

全站热榜应优先命中 `idx_status_score`：

```sql
EXPLAIN SELECT id, circle_id, title, content, author_id, publish_time, current_score,
               interaction_count, status, created_at, updated_at
FROM topic
WHERE status = 1
ORDER BY current_score DESC
LIMIT 50;
```

圈子热榜应优先命中 `idx_circle_status_score`：

```sql
EXPLAIN SELECT id, circle_id, title, content, author_id, publish_time, current_score,
               interaction_count, status, created_at, updated_at
FROM topic
WHERE circle_id = 1
  AND status = 1
ORDER BY current_score DESC
LIMIT 20;
```

新星榜应使用 `idx_newcomer_rank` 缩小 24 小时窗口：

```sql
EXPLAIN SELECT id, circle_id, title, content, author_id, publish_time, current_score,
               interaction_count, status, created_at, updated_at
FROM topic
WHERE status = 1
  AND publish_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
ORDER BY current_score DESC
LIMIT 10;
```

## 聚合与分析

热度聚合应使用 `idx_created_topic_type` 或同等时间窗口索引：

```sql
EXPLAIN SELECT topic_id, interaction_type, COUNT(*) AS total_count
FROM interaction_event
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)
  AND created_at < NOW()
GROUP BY topic_id, interaction_type;
```

互动类型分析应使用 `idx_created_type`：

```sql
EXPLAIN SELECT interaction_type, COUNT(*) AS total_count
FROM interaction_event
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
  AND created_at < NOW()
GROUP BY interaction_type;
```

反作弊统计应使用 `idx_valid_created_topic`：

```sql
EXPLAIN SELECT id, user_id, topic_id, interaction_type, device_fingerprint, ip_address,
               is_valid, invalid_reason, created_at
FROM user_behavior
WHERE is_valid = 0
  AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)
  AND created_at < NOW()
ORDER BY created_at DESC;
```

## 判断标准

- `type` 尽量为 `range`、`ref` 或更优。
- `key` 应落到上方列出的预期索引之一。
- `rows` 不应接近整表规模；如果接近，需要检查数据分布或补充更窄的时间条件。
- `Extra` 中出现 `Using filesort` 时要结合返回行数判断风险；小结果集可以接受，大结果集需要继续优化。
