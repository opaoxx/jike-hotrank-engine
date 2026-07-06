# Jike HotRank Engine — API 参考文档

> 面向前端开发的完整 API 参考。所有接口均返回统一信封 `ApiResponse<T>`。

## 目录

- [1. 通用约定](#1-通用约定)
- [2. 榜单接口 — RankingController](#2-榜单接口--rankingcontroller)
- [3. 话题接口 — TopicController](#3-话题接口--topiccontroller)
- [4. 互动接口 — InteractionController](#4-互动接口--interactioncontroller)
- [5. 分析接口 — AnalysisController](#5-分析接口--analysiscontroller)
- [6. 反作弊接口 — AntiSpamController](#6-反作弊接口--antispamcontroller)
- [7. SSE 实时推送 — RankingNotificationController](#7-sse-实时推送--rankingnotificationcontroller)
- [8. 运维接口 — OperationsController](#8-运维接口--operationscontroller)
- [9. 性能接口 — PerfController](#9-性能接口--perfcontroller)

---

## 1. 通用约定

### 1.1 响应信封 ApiResponse\<T\>

所有接口响应均为以下 JSON 结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | `0` = 成功；非 0 = 错误 |
| `message` | string | 成功时为 `"success"`，失败时为具体错误描述 |
| `data` | T / null | 响应载荷，类型视具体接口而定；无数据时为 `null` |

### 1.2 错误码一览

| code | HTTP Status | 场景 |
|------|-------------|------|
| 400 | 400 | 请求参数校验失败（类型错误、缺少必填项等） |
| 403 | 403 | 话题已被屏蔽 |
| 404 | 404 | 话题不存在 |
| 429 | 429 | 互动频率超限（单话题 24h 内超 50 次） |
| 500 | 500 | 服务器内部错误 |

### 1.3 互动类型编码

| 值 | 含义 | 热度权重 |
|----|------|----------|
| `1` | 点赞 (like) | ×1 |
| `2` | 收藏 (bookmark) | ×2 |
| `3` | 转发 (share) | ×3 |
| `5` | 评论 (comment) | ×5 |

> 注意：值 `4` 被保留未使用，传值会被拒绝。

### 1.4 话题状态编码

| 值 | 含义 |
|----|------|
| `0` | 已屏蔽 (blocked) |
| `1` | 正常 (normal) |
| `2` | 待审核 (under review) |

### 1.5 数据格式约定

| Java 类型 | JSON 序列化 |
|-----------|-------------|
| `Long` | number |
| `BigDecimal` | number（可能有 4 位小数） |
| `LocalDateTime` | string，ISO-8601 格式 `"2026-07-06T15:30:00"` |
| `Integer` | number |
| `String` | string |

---

## 2. 榜单接口 — RankingController

Base: `/api/ranking`

所有榜单响应均为 `ApiResponse<RankingResponseDTO>`。

### RankingResponseDTO 结构

```json
{
  "rankingType": "global",
  "circleId": null,
  "circleName": null,
  "userId": null,
  "updateTime": "2026-07-06T15:30:00",
  "items": []
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `rankingType` | string | `"global"` / `"circle"` / `"newcomer"` / `"surging"` / `"personalized"` |
| `circleId` | number\|null | 仅 circle 类型有值 |
| `circleName` | string\|null | 仅 circle 类型有值 |
| `userId` | number\|null | 仅 personalized 类型有值 |
| `updateTime` | string | 榜单生成时间（ISO-8601） |
| `items` | RankingItemDTO[] | 排名项列表 |

### RankingItemDTO 结构

```json
{
  "rank": 1,
  "topicId": 1,
  "title": "话题标题",
  "circleId": 1,
  "circleName": "科技圈",
  "score": 1234.5678,
  "interactionCount": 892,
  "authorId": 1001,
  "publishTime": "2026-07-06T10:00:00"
}
```

---

### 2.1 全站热榜

```http
GET /api/ranking/global?limit=50
```

| 参数 | 位置 | 类型 | 必填 | 默认值 | 范围 | 说明 |
|------|------|------|------|--------|------|------|
| `limit` | query | int | 否 | 50 | 1~100 | 返回条数 |

**调用示例：**

```bash
curl http://localhost:8080/api/ranking/global?limit=20
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "rankingType": "global",
    "updateTime": "2026-07-06T15:30:00",
    "items": [
      {
        "rank": 1,
        "topicId": 5,
        "title": "GPT-5 正式发布",
        "circleId": 1,
        "circleName": "科技圈",
        "score": 2345.6789,
        "interactionCount": 1523,
        "authorId": 9001,
        "publishTime": "2026-07-06T08:00:00"
      }
    ]
  }
}
```

---

### 2.2 圈子热榜

```http
GET /api/ranking/circle/{circleId}?limit=20
```

| 参数 | 位置 | 类型 | 必填 | 默认值 | 范围 | 说明 |
|------|------|------|------|--------|------|------|
| `circleId` | path | long | 是 | — | — | 圈子 ID |
| `limit` | query | int | 否 | 20 | 1~100 | 返回条数 |

**调用示例：**

```bash
curl http://localhost:8080/api/ranking/circle/1?limit=20
```

**响应：** `rankingType` = `"circle"`，`circleId` 和 `circleName` 有值。

---

### 2.3 新星榜（24h 内发布的话题）

```http
GET /api/ranking/newcomer?limit=10
```

| 参数 | 位置 | 类型 | 必填 | 默认值 | 范围 |
|------|------|------|------|--------|------|
| `limit` | query | int | 否 | 10 | 1~50 |

**响应：** `rankingType` = `"newcomer"`。

---

### 2.4 飙升榜（近 1h 增速最快）

```http
GET /api/ranking/surging?limit=10
```

| 参数 | 位置 | 类型 | 必填 | 默认值 | 范围 |
|------|------|------|------|--------|------|
| `limit` | query | int | 否 | 10 | 1~50 |

**响应：** `rankingType` = `"surging"`。数据按当前小时加权得分与前 1 小时的比值排序。

---

### 2.5 个性化榜单

```http
GET /api/ranking/personalized?userId=9001&limit=20
```

| 参数 | 位置 | 类型 | 必填 | 默认值 | 范围 | 说明 |
|------|------|------|------|--------|------|------|
| `userId` | query | long | **是** | — | — | 用户 ID |
| `limit` | query | int | 否 | 20 | 1~100 | 返回条数 |

**说明：** 在全站热榜基础上，根据用户在各大圈的偏好权重（`user_circle_preference.weight`）对得分进行二次加权排序。

**响应：** `rankingType` = `"personalized"`，`userId` 有值。

---

### 2.6 缓存行为

所有榜单接口使用 JVM 本地缓存（约 5s TTL，含随机抖动避免缓存雪崩）。热度聚合定时任务（每 5 分钟）执行后主动清空缓存。

---

## 3. 话题接口 — TopicController

Base: `/api/topic`

### 3.1 获取话题详情

```http
GET /api/topic/{id}
```

| 参数 | 位置 | 类型 | 必填 |
|------|------|------|------|
| `id` | path | long | 是 |

**调用示例：**

```bash
curl http://localhost:8080/api/topic/1
```

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "circleId": 1,
    "title": "GPT-5 正式发布",
    "content": "OpenAI 今日正式发布 GPT-5 模型，支持多模态原生推理……",
    "authorId": 9001,
    "publishTime": "2026-07-06T08:00:00",
    "currentScore": 2345.6789,
    "interactionCount": 1523,
    "status": 1,
    "createdAt": "2026-07-06T08:00:00",
    "updatedAt": "2026-07-06T15:30:00"
  }
}
```

**错误：** `404` — 话题不存在。

---

### 3.2 屏蔽话题

```http
POST /api/topic/{id}/block
```

| 参数 | 位置 | 类型 | 必填 |
|------|------|------|------|
| `id` | path | long | 是 |

将话题状态设为 `0`（已屏蔽），并清空所有榜单缓存。屏蔽后的话题不会出现在任何榜单中。

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

### 3.3 取消屏蔽话题

```http
POST /api/topic/{id}/unblock
```

| 参数 | 位置 | 类型 | 必填 |
|------|------|------|------|
| `id` | path | long | 是 |

将话题状态复原为 `1`（正常），并清空所有榜单缓存。

---

## 4. 互动接口 — InteractionController

### 4.1 记录互动事件

```http
POST /api/interaction
```

**Content-Type:** `application/json`

**请求体：**

```json
{
  "topicId": 1,
  "userId": 9001,
  "interactionType": 1,
  "deviceFingerprint": "device_abc123",
  "ipAddress": "192.168.1.100"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `topicId` | long | **是** | 话题 ID |
| `userId` | long | **是** | 用户 ID |
| `interactionType` | int | **是** | 互动类型：`1`/`2`/`3`/`5` |
| `deviceFingerprint` | string | 否 | 设备指纹（防刷用） |
| `ipAddress` | string | 否 | IP 地址（防刷用） |

**调用示例：**

```bash
curl -X POST http://localhost:8080/api/interaction \
  -H "Content-Type: application/json" \
  -d '{"topicId":1, "userId":9001, "interactionType":1, "deviceFingerprint":"device_001", "ipAddress":"192.168.1.100"}'
```

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 5001,
    "topicId": 1,
    "userId": 9001,
    "interactionType": 1,
    "deviceFingerprint": "device_001",
    "ipAddress": "192.168.1.100",
    "weightMultiplier": 1.000,
    "createdAt": "2026-07-06T16:00:00"
  }
}
```

**可能错误：**
- `400` — 参数非法（如 `interactionType=4` 或 `topicId` 缺失）
- `403` — 话题已被屏蔽
- `404` — 话题不存在
- `429` — 同用户同话题 24h 内互动超 50 次

---

## 5. 分析接口 — AnalysisController

Base: `/api/analysis`

所有分析接口响应为 `ApiResponse<Map<String, Object>>`。

---

### 5.1 热度分布

```http
GET /api/analysis/heat-distribution
```

**响应 `data` 字段：**

```json
{
  "topicCount": 50,
  "ranges": [
    { "range": "0~10", "count": 15, "percentage": 30.0 },
    { "range": "10~50", "count": 20, "percentage": 40.0 },
    { "range": "50~100", "count": 8, "percentage": 16.0 },
    { "range": "100~500", "count": 5, "percentage": 10.0 },
    { "range": "500+", "count": 2, "percentage": 4.0 }
  ],
  "maxScore": 2345.6789,
  "avgScore": 85.32,
  "medianScore": 42.10,
  "maxTopic": {
    "topicId": 5,
    "title": "GPT-5 正式发布",
    "score": 2345.6789
  }
}
```

---

### 5.2 互动统计

```http
GET /api/analysis/interaction-stats?hours=24
```

| 参数 | 位置 | 类型 | 必填 | 默认值 | 范围 |
|------|------|------|------|--------|------|
| `hours` | query | int | 否 | 24 | 1~168 |

**响应 `data` 字段：**

```json
{
  "hours": 24,
  "periodStart": "2026-07-05T16:00:00",
  "periodEnd": "2026-07-06T16:00:00",
  "total": 5000,
  "byType": [
    { "type": 1, "name": "like", "count": 2500, "percentage": 50.0 },
    { "type": 2, "name": "bookmark", "count": 1200, "percentage": 24.0 },
    { "type": 3, "name": "share", "count": 800, "percentage": 16.0 },
    { "type": 5, "name": "comment", "count": 500, "percentage": 10.0 }
  ]
}
```

---

### 5.3 圈子活跃度

```http
GET /api/analysis/circle-activity
```

**响应 `data` 字段：**

```json
{
  "circleCount": 5,
  "items": [
    {
      "circleId": 1,
      "circleName": "科技圈",
      "topicCount": 15,
      "avgScore": 234.56,
      "interactionCount": 3200,
      "rank": 1
    }
  ]
}
```

---

### 5.4 反作弊统计

```http
GET /api/analysis/anti-cheat-stats?days=7
```

| 参数 | 位置 | 类型 | 必填 | 默认值 | 范围 |
|------|------|------|------|--------|------|
| `days` | query | int | 否 | 7 | 1~30 |

**响应 `data` 字段：**

```json
{
  "days": 7,
  "periodStart": "2026-06-29T16:00:00",
  "periodEnd": "2026-07-06T16:00:00",
  "totalBlockedCount": 123,
  "byReason": [
    { "reason": "frequency_limit", "count": 80 },
    { "reason": "device_penalty", "count": 30 },
    { "reason": "anomaly_surge", "count": 13 }
  ],
  "dailyTrend": [
    { "date": "2026-07-06", "count": 15 }
  ],
  "topTopics": [
    { "topicId": 3, "blockedCount": 12 }
  ]
}
```

---

### 5.5 总览（组合接口）

```http
GET /api/analysis/overview
```

一次返回以上全部 4 个分析接口的数据 + 缓存统计。适合 Dashboard 首页加载。

**响应 `data` 字段：**

```text
{
  "heatDistribution": { },
  "interactionStats": { },
  "circleActivity": { },
  "antiCheatStats": { },
  "cacheStats": {
    "size": 5,
    "hits": 1234,
    "misses": 56,
    "nullHits": 0,
    "hitRate": 0.9566
  }
}
```

---

## 6. 反作弊接口 — AntiSpamController

### 6.1 当日反作弊报告

```http
GET /api/anti-spam/report
```

**响应 `data` 字段：**

```json
{
  "totalBlockedCount": 25,
  "blockReasonStats": {
    "frequency_limit": 18,
    "device_penalty": 5,
    "anomaly_surge": 2
  },
  "affectedTopicCount": 8,
  "affectedTopicIds": [1, 3, 5, 7],
  "suspiciousUsers": [
    { "userId": 9001, "blockCount": 7 },
    { "userId": 9002, "blockCount": 5 }
  ],
  "interactionTypeStats": {
    "1": { "type": 1, "name": "like", "blockedCount": 15 },
    "2": { "type": 2, "name": "bookmark", "blockedCount": 5 },
    "3": { "type": 3, "name": "share", "blockedCount": 3 },
    "5": { "type": 5, "name": "comment", "blockedCount": 2 }
  }
}
```

---

## 7. SSE 实时推送 — RankingNotificationController

### 7.1 订阅榜单事件流

```http
GET /api/notifications/rankings/stream
```

**响应类型：** `text/event-stream` (Server-Sent Events)

**连接方式（前端）：**

```javascript
const es = new EventSource('/api/notifications/rankings/stream');
es.addEventListener('connected', e => { /* handle connected */ });
es.addEventListener('ranking-updated', e => { /* handle ranking-updated */ });
es.addEventListener('top-n-entered', e => { /* handle top-n-entered */ });
```

**事件一览：**

| 事件名 | 触发时机 | 载荷 |
|--------|----------|------|
| `connected` | 客户端首次建立连接 | `{ "subscriberCount": 1 }` |
| `ranking-updated` | 热度聚合任务完成（每 5min） | `{ "updatedTopicCount": 50 }` |
| `top-n-entered` | 有新话题进入前 N 名 | `{ "threshold": 10, "topicId": 5, "title": "...", "score": 2345.67 }` |

**错误处理：** 连接中断后客户端应实现自动重连。建议使用指数退避：
- 首次重连：1s
- 第 2 次：2s
- 第 3 次：4s
- 后续：最大 30s

**超时：** 服务端 30min 无活动后主动关闭连接，客户端应监听 `onerror` 事件并重连。

---

## 8. 运维接口 — OperationsController

Base: `/api/ops`

这两个接口需鉴权 token，用于演示和运维。

### 8.1 手动触发热度聚合

```http
POST /api/ops/heat-aggregation?token=ops_demo_token
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `token` | query | string | 是 | 固定值：`ops_demo_token` |

立即执行一次热度聚合计算（与每 5 分钟定时任务相同逻辑，受 MySQL 命名锁保护不会重复执行）。

### 8.2 手动触发快照

```http
POST /api/ops/snapshot?token=ops_demo_token
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|------|------|------|------|------|
| `token` | query | string | 是 | 固定值：`ops_demo_token` |

立即创建一次 Top 100 榜单快照存入 `topic_score_snapshot` 表。

---

## 9. 性能接口 — PerfController

Base: `/api/perf`

### 9.1 缓存统计

```http
GET /api/perf/cache-comparison
```

**响应 `data` 字段：**

```json
{
  "size": 5,
  "hits": 1234,
  "misses": 56,
  "nullHits": 0,
  "hitRate": 0.9566
}
```

### 9.2 内部压测（演示用）

```http
POST /api/perf/load-test?qps=20&duration=5&token=perf_test_token
```

| 参数 | 位置 | 类型 | 必填 | 默认值 | 范围 | 说明 |
|------|------|------|------|--------|------|------|
| `qps` | query | int | 否 | 20 | 1~200 | 每秒请求数 |
| `duration` | query | int | 否 | 5 | 1~30 | 持续秒数 |
| `baseUrl` | query | string | 否 | `http://localhost:8080` | — | 目标地址 |
| `token` | query | string | 是 | — | — | 固定值：`perf_test_token` |

> 该接口会从后端发起 HTTP 请求攻击自身，是演示/测试工具，非生产功能。

---

## 附录：快速参考卡片

```
所有接口 Base URL:     http://localhost:8080

榜单:
  GET  /api/ranking/global?limit=50         全站热榜
  GET  /api/ranking/circle/{id}?limit=20    圈子热榜
  GET  /api/ranking/newcomer?limit=10       新星榜
  GET  /api/ranking/surging?limit=10        飙升榜
  GET  /api/ranking/personalized?userId=X   个性化榜

话题:
  GET  /api/topic/{id}                      查看话题
  POST /api/topic/{id}/block                屏蔽话题
  POST /api/topic/{id}/unblock              取消屏蔽

互动:
  POST /api/interaction                     记录互动

分析:
  GET  /api/analysis/overview               总览
  GET  /api/analysis/heat-distribution      热度分布
  GET  /api/analysis/interaction-stats      互动统计
  GET  /api/analysis/circle-activity        圈子活跃度
  GET  /api/analysis/anti-cheat-stats       反作弊统计

实时:
  GET  /api/notifications/rankings/stream   SSE 榜单事件流

反作弊:
  GET  /api/anti-spam/report                当日报告

运维:
  POST /api/ops/heat-aggregation            手动热度聚合
  POST /api/ops/snapshot                    手动快照

性能:
  GET  /api/perf/cache-comparison           缓存统计
```
