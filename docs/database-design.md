# 数据库设计文档

## 1. 数据库概述

- **数据库名**：jike_hotrank
- **字符集**：utf8mb4
- **排序规则**：utf8mb4_unicode_ci
- **存储引擎**：InnoDB

---

## 2. 表结构设计

### 2.1 圈子表 (circle)

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | AUTO_INCREMENT | 圈子ID（主键） |
| name | VARCHAR(100) | 是 | - | 圈子名称（唯一） |
| description | VARCHAR(500) | 否 | NULL | 圈子描述 |
| icon_url | VARCHAR(255) | 否 | NULL | 圈子图标URL |
| status | TINYINT | 是 | 1 | 状态：0-禁用 1-启用 |
| sort_order | INT | 是 | 0 | 排序权重 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间（自动更新） |

**索引：**
- PRIMARY KEY (id)
- UNIQUE KEY uk_name (name)
- INDEX idx_status_sort (status, sort_order)

---

### 2.2 话题表 (topic)

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | AUTO_INCREMENT | 话题ID（主键） |
| circle_id | BIGINT | 是 | - | 所属圈子ID |
| title | VARCHAR(200) | 是 | - | 话题标题 |
| content | TEXT | 否 | NULL | 话题内容 |
| author_id | BIGINT | 是 | - | 作者用户ID |
| publish_time | DATETIME | 是 | - | 发布时间 |
| current_score | DECIMAL(20,4) | 是 | 0 | 当前热度分 |
| interaction_count | INT | 是 | 0 | 总互动次数 |
| status | TINYINT | 是 | 1 | 状态：0-屏蔽 1-正常 2-待审核 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间（自动更新） |

**索引：**
- PRIMARY KEY (id)
- INDEX idx_circle_score (circle_id, current_score DESC)
- INDEX idx_publish_time (publish_time)
- INDEX idx_status (status)
- INDEX idx_author (author_id)

---

### 2.3 互动事件表 (interaction_event)

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | AUTO_INCREMENT | 事件ID（主键） |
| topic_id | BIGINT | 是 | - | 话题ID |
| user_id | BIGINT | 是 | - | 用户ID |
| interaction_type | TINYINT | 是 | - | 互动类型：1-点赞 2-收藏 3-转发 5-评论 |
| device_fingerprint | VARCHAR(100) | 否 | NULL | 设备指纹 |
| ip_address | VARCHAR(50) | 否 | NULL | IP地址 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- PRIMARY KEY (id)
- INDEX idx_topic_created (topic_id, created_at)
- INDEX idx_user_topic (user_id, topic_id)
- INDEX idx_created_at (created_at)
- INDEX idx_device (device_fingerprint)

---

### 2.4 用户行为表 (user_behavior)

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | AUTO_INCREMENT | 行为ID（主键） |
| user_id | BIGINT | 是 | - | 用户ID |
| topic_id | BIGINT | 是 | - | 话题ID |
| interaction_type | TINYINT | 是 | - | 互动类型 |
| device_fingerprint | VARCHAR(100) | 否 | NULL | 设备指纹 |
| ip_address | VARCHAR(50) | 否 | NULL | IP地址 |
| is_valid | TINYINT | 是 | 1 | 是否有效：0-无效 1-有效 |
| invalid_reason | VARCHAR(200) | 否 | NULL | 无效原因 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- PRIMARY KEY (id)
- INDEX idx_user_created (user_id, created_at)
- INDEX idx_topic_created (topic_id, created_at)
- INDEX idx_device (device_fingerprint)
- INDEX idx_is_valid (is_valid)

---

### 2.5 话题热度快照表 (topic_score_snapshot)

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | AUTO_INCREMENT | 快照ID（主键） |
| topic_id | BIGINT | 是 | - | 话题ID |
| circle_id | BIGINT | 是 | - | 圈子ID |
| score | DECIMAL(20,4) | 是 | - | 热度分 |
| rank_position | INT | 是 | - | 排名位置 |
| snapshot_time | DATETIME | 是 | - | 快照时间 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引：**
- PRIMARY KEY (id)
- INDEX idx_snapshot_time (snapshot_time)
- INDEX idx_topic_snapshot (topic_id, snapshot_time)
- INDEX idx_circle_snapshot (circle_id, snapshot_time, rank_position)

---

### 2.6 用户圈子偏好表 (user_circle_preference)

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | AUTO_INCREMENT | 偏好ID（主键） |
| user_id | BIGINT | 是 | - | 用户ID |
| circle_id | BIGINT | 是 | - | 圈子ID |
| weight | DECIMAL(5,2) | 是 | 1.00 | 偏好权重（0.01-10.00） |
| interaction_count | INT | 是 | 0 | 互动次数 |
| created_at | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间（自动更新） |

**索引：**
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_circle (user_id, circle_id)
- INDEX idx_user_weight (user_id, weight DESC)

---

## 3. ER关系图

```
circle 1:N topic
topic 1:N interaction_event
topic 1:N user_behavior
topic 1:N topic_score_snapshot
user N:M circle (通过 user_circle_preference)
```

---

## 4. 慢查询优化

### 4.1 全站热榜查询
```sql
SELECT * FROM topic WHERE status = 1 ORDER BY current_score DESC LIMIT 50;
```
**优化**：使用 idx_circle_score 索引，避免全表扫描

### 4.2 互动事件聚合
```sql
SELECT topic_id, interaction_type, COUNT(*)
FROM interaction_event
WHERE created_at >= ? AND created_at < ?
GROUP BY topic_id, interaction_type;
```
**优化**：使用 idx_topic_created 复合索引

### 4.3 频率限制检查
```sql
SELECT COUNT(*) FROM interaction_event
WHERE user_id = ? AND topic_id = ? AND created_at >= ?;
```
**优化**：使用 idx_user_topic 复合索引
