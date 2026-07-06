# Day6 答辩 Demo 手册

本文档用于 15 分钟答辩演示。核心目标是把三条业务链路讲清楚：互动写入如何影响热度、防刷如何留下审计证据、话题屏蔽如何立即影响榜单。

## 演示前准备

1. 初始化数据库。

```powershell
mysql -u root -p < src/main/resources/sql/schema.sql
mysql -u root -p < src/main/resources/sql/data.sql
```

2. 启动应用。

```powershell
./mvnw spring-boot:run
```

3. 可选：打开 SSE 订阅窗口，观察榜单更新事件。

```powershell
curl.exe -N "http://localhost:8080/api/notifications/rankings/stream"
```

4. Demo 手动触发任务使用运维 token，默认值见 `jike-hotrank.operations.token`。

```text
ops_demo_token
```

## 场景一：互动触发热度变化

目标：证明互动事件会写入流水，聚合后影响全站榜单。

1. 查询当前全站热榜。

```powershell
curl.exe "http://localhost:8080/api/ranking/global?limit=5"
```

2. 对 `topicId=1` 连续写入高权重评论和转发。

```powershell
curl.exe -X POST "http://localhost:8080/api/interaction" -H "Content-Type: application/json" -d "{\"topicId\":1,\"userId\":8101,\"interactionType\":5,\"deviceFingerprint\":\"demo_device_8101\",\"ipAddress\":\"127.0.0.1\"}"
curl.exe -X POST "http://localhost:8080/api/interaction" -H "Content-Type: application/json" -d "{\"topicId\":1,\"userId\":8102,\"interactionType\":3,\"deviceFingerprint\":\"demo_device_8102\",\"ipAddress\":\"127.0.0.1\"}"
```

3. 手动触发一次热度聚合，避免现场等待 5 分钟定时任务。

```powershell
curl.exe -X POST "http://localhost:8080/api/ops/heat-aggregation?token=ops_demo_token"
```

4. 再次查询热榜。

```powershell
curl.exe "http://localhost:8080/api/ranking/global?limit=5"
```

观察点：

- `topicId=1` 的 `interactionCount` 或 `score` 应上升。
- SSE 窗口应收到 `ranking-updated` 事件。
- 说明热度公式：`weightedInteractionScore / (publishHours + 2)^1.8`。

## 场景二：刷热度被拦截并记录

目标：证明同一用户对同一话题短时间高频互动不会无限计入热度，并会留下审计记录。

1. 连续对 `topicId=1` 写入多次互动。默认配置为 24 小时最多 50 次有效互动；正式演示可把 `jike-hotrank.anti-spam.frequency.max-interactions` 临时调小到 `3`，重启后执行 5 次即可触发。若不改配置，把循环次数改为 `55`。

```powershell
1..5 | ForEach-Object {
  curl.exe -X POST "http://localhost:8080/api/interaction" -H "Content-Type: application/json" -d "{\"topicId\":1,\"userId\":8201,\"interactionType\":1,\"deviceFingerprint\":\"spam_device_8201\",\"ipAddress\":\"127.0.0.2\"}"
}
```

2. 查询防刷报告。

```powershell
curl.exe "http://localhost:8080/api/anti-spam/report"
curl.exe "http://localhost:8080/api/analysis/anti-cheat-stats?days=7"
```

观察点：

- 超限行为返回业务错误，不进入有效互动流水。
- `user_behavior` 中保留无效原因，报告接口能看到拦截类型和涉及话题。
- 设备指纹风险不会一刀切拒绝，而是通过 `weight_multiplier` 降低热度贡献。

## 场景三：话题屏蔽立即生效

目标：证明运营屏蔽话题后，榜单缓存会被清理，话题立即从榜单消失。

1. 查询游戏圈榜单，确认 `topicId=4` 当前可见。

```powershell
curl.exe "http://localhost:8080/api/ranking/circle/2?limit=5"
```

2. 屏蔽 `topicId=4`。

```powershell
curl.exe -X POST "http://localhost:8080/api/topic/4/block"
```

3. 重新查询游戏圈榜单。

```powershell
curl.exe "http://localhost:8080/api/ranking/circle/2?limit=5"
```

4. 演示结束后恢复话题。

```powershell
curl.exe -X POST "http://localhost:8080/api/topic/4/unblock"
curl.exe -X POST "http://localhost:8080/api/ops/heat-aggregation?token=ops_demo_token"
```

观察点：

- 屏蔽接口会调用榜单缓存清理。
- 榜单接口只返回 `status=1` 的话题。
- 恢复后重新聚合即可回到正常展示。

## 现场排查

| 现象 | 优先检查 |
| --- | --- |
| 榜单没有变化 | 是否执行了 `/api/ops/heat-aggregation`，数据库中是否存在新互动 |
| 运维触发返回 403 | `token` 是否等于 `jike-hotrank.operations.token` |
| 防刷不触发 | 本地配置的 `max-interactions` 是否过大，是否重启应用 |
| 屏蔽后仍可见 | 是否查询的是缓存命中结果；屏蔽接口成功时会清理缓存 |
| SSE 没有消息 | 确认 SSE 窗口先打开，再触发聚合或 TOP10 变化 |
