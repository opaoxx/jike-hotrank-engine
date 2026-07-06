# SQL 执行说明

本目录提供两种入口：新库初始化和旧库升级。不要把所有 `.sql` 文件按文件名全量执行一遍。

## 场景一：新库或演示库重置

适用于没有历史数据，或者可以清空重建的本地演示环境。

从项目根目录依次执行：

```bash
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/data.sql
```

IDEA Database Console 用户请按顺序分别打开并执行 `schema.sql`、`data.sql`。

这个入口会依次执行：

1. `schema.sql`
2. `data.sql`

注意：`schema.sql` 会删除并重建业务表，已有数据会丢失。

## 场景二：已有旧库升级

适用于数据库里已经有早期 5 张表或旧测试数据，需要保留数据并升级到当前项目结构。

从项目根目录依次执行：

```bash
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/20260706_upgrade_existing_database.sql
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/repair.sql
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/20260706_drop_legacy_redundant_indexes.sql
```

IDEA Database Console 用户请按顺序分别打开并执行三个脚本。

这个入口会依次执行：

1. `20260706_upgrade_existing_database.sql`
2. `repair.sql`
3. `20260706_drop_legacy_redundant_indexes.sql`

升级内容：

- 补齐 `interaction_event.weight_multiplier`。
- 补齐 `user_circle_preference` 表。
- 补齐当前查询需要的复合索引。
- 回填缺失的有效行为审计。
- 重算 `topic.interaction_count` 和 `topic.current_score`。
- 重建用户圈子偏好。
- 清理旧 SQL 遗留的冗余索引。

## 单步脚本说明

| 文件 | 用途 | 是否会清空数据 |
| --- | --- | --- |
| `00_setup_fresh_database.sql` | 新库一键初始化入口 | 是 |
| `01_upgrade_existing_database.sql` | 旧库一键升级入口 | 否 |
| `schema.sql` | 创建最新表结构 | 是 |
| `data.sql` | 导入演示数据 | 是 |
| `20260706_upgrade_existing_database.sql` | 幂等补齐旧库字段、表、索引 | 否 |
| `repair.sql` | 修复历史数据和重建偏好 | 不清空主数据 |
| `20260706_drop_legacy_redundant_indexes.sql` | 清理旧库冗余索引 | 否 |

## 执行后检查

```sql
SELECT 'circle' table_name, COUNT(*) row_count FROM circle
UNION ALL SELECT 'topic', COUNT(*) FROM topic
UNION ALL SELECT 'interaction_event', COUNT(*) FROM interaction_event
UNION ALL SELECT 'user_behavior', COUNT(*) FROM user_behavior
UNION ALL SELECT 'topic_score_snapshot', COUNT(*) FROM topic_score_snapshot
UNION ALL SELECT 'user_circle_preference', COUNT(*) FROM user_circle_preference;
```

旧库升级后，下面查询应返回空结果：

```sql
SELECT t.id, t.title, t.interaction_count AS recorded_count, COUNT(ie.id) AS actual_count
FROM topic t
LEFT JOIN interaction_event ie ON t.id = ie.topic_id
GROUP BY t.id, t.title, t.interaction_count
HAVING t.interaction_count <> COUNT(ie.id);
```
