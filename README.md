# 即刻 App 内容社区实时热点榜单引擎

Jike HotRank Engine 是一个基于 Spring Boot、MyBatis 和 MySQL 的内容社区实时热点榜单后端。项目围绕互动事件采集、热度聚合、多维榜单、反作弊、个性化排序、缓存和实时推送展开，适合作为 7 天课程项目的后端主线。

## 技术栈

| 模块 | 选型 |
| --- | --- |
| Java | JDK 21 |
| Web 框架 | Spring Boot 4.0.6 |
| ORM | MyBatis 4.0.1 |
| 数据库 | MySQL 8.0 |
| 缓存 | JVM 本地缓存 |
| 定时任务 | Spring Scheduling |
| 测试 | JUnit 5 + Mockito |

## 快速开始

```bash
mysql -u root -p < src/main/resources/sql/schema.sql
mysql -u root -p < src/main/resources/sql/data.sql
./mvnw spring-boot:run
```

默认配置位于 `src/main/resources/application.yml`。本地数据库默认连接：

```yaml
spring.datasource.url: jdbc:mysql://localhost:3306/jike_hotrank
spring.datasource.username: root
spring.datasource.password: root
```

已有数据库升级时，按需执行：

```bash
mysql -u root -p < src/main/resources/sql/20260706_add_interaction_weight_multiplier.sql
mysql -u root -p < src/main/resources/sql/20260706_add_rank_query_indexes.sql
mysql -u root -p < src/main/resources/sql/20260706_add_analysis_query_indexes.sql
```

## 核心能力

- 记录点赞、收藏、转发、评论四类互动事件。
- 按互动权重、防刷倍率和发布时间衰减计算话题热度。
- 提供全站热榜、圈子热榜、新星榜、飙升榜和个性化热榜。
- 支持频率限制、设备指纹降权、异常突增审核标记。
- 使用本地缓存降低榜单查询压力，并缓存空结果避免穿透。
- 热度聚合后清理榜单缓存，并通过 SSE 推送榜单变化事件。
- 定时聚合与快照任务使用 MySQL named lock 避免多实例重复执行。

## API

### 互动事件

`POST /api/interaction`

```json
{
  "topicId": 1,
  "userId": 9001,
  "interactionType": 1,
  "deviceFingerprint": "device_001",
  "ipAddress": "192.168.1.100"
}
```

互动类型：`1` 点赞，`2` 收藏，`3` 转发，`5` 评论。

### 榜单查询

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/ranking/global?limit=50` | 全站热榜 |
| GET | `/api/ranking/circle/{circleId}?limit=20` | 圈子热榜 |
| GET | `/api/ranking/newcomer?limit=10` | 24 小时新星榜 |
| GET | `/api/ranking/surging?limit=10` | 最近 1 小时飙升榜 |
| GET | `/api/ranking/personalized?userId={userId}&limit=50` | 个性化热榜 |

`limit` 会按接口类型归一化并限制最大值，避免查询和缓存 key 被异常参数放大。

### 话题管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/topic/{id}` | 查询话题详情 |
| POST | `/api/topic/{id}/block` | 屏蔽话题并清理榜单缓存 |
| POST | `/api/topic/{id}/unblock` | 恢复话题并清理榜单缓存 |

### 反作弊与实时推送

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/anti-spam/report` | 查看反作弊报告 |
| GET | `/api/notifications/rankings/stream` | 订阅榜单 SSE 事件 |

SSE 事件名包括 `ranking-updated` 和 `top-n-entered`。

### 性能与数据分析

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/perf/load-test?qps=20&duration=5&token=perf_test_token` | 触发受限的轻量压测 |
| GET | `/api/perf/cache-comparison` | 查看榜单缓存统计 |
| GET | `/api/analysis/heat-distribution` | 热度分布分析 |
| GET | `/api/analysis/interaction-stats?hours=24` | 互动类型统计 |
| GET | `/api/analysis/circle-activity` | 圈子活跃度分析 |
| GET | `/api/analysis/anti-cheat-stats?days=7` | 反作弊统计 |
| GET | `/api/analysis/overview` | 分析总览 |

### 运维与演示触发

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/ops/heat-aggregation?token=ops_demo_token` | 手动触发热度聚合，用于 Demo 或本地验证 |
| POST | `/api/ops/snapshot?token=ops_demo_token` | 手动生成榜单快照，用于 Demo 或本地验证 |

默认 token 位于 `jike-hotrank.operations.token`。生产环境应替换为正式后台权限体系。

## 热度算法

```text
score = weightedInteractionScore / (publishHours + 2)^1.8
```

默认互动权重：

| 类型 | 权重 |
| --- | --- |
| 点赞 | 1 |
| 收藏 | 2 |
| 转发 | 3 |
| 评论 | 5 |

反作弊不会简单丢弃所有可疑互动：频率超限会拒绝写入；设备指纹命中多账号风险时，互动仍会记录，但通过 `weight_multiplier` 对热度贡献降权。

## 常用脚本

| 脚本 | 用途 |
| --- | --- |
| `src/main/resources/sql/schema.sql` | 初始化数据库表结构 |
| `src/main/resources/sql/data.sql` | 初始化本地演示数据 |
| `src/main/resources/sql/repair.sql` | 修复历史互动数、热度分、偏好权重和孤立数据 |
| `src/main/resources/sql/20260706_add_analysis_query_indexes.sql` | 为 Day5 分析和审计查询补索引 |
| `docs/loadtest/benchmark.bat` | Windows 压测入口 |
| `docs/loadtest/benchmark.sh` | Linux/macOS 压测入口 |
| `docs/performance-analysis.md` | Day5 性能与数据分析说明 |
| `docs/sql-explain-checklist.md` | SQL 执行计划检查清单 |
| `docs/demo-guide.md` | Day6 答辩 Demo 操作手册 |
| `docs/final-review-checklist.md` | Day6 小组互评与终审清单 |
| `docs/presentation-outline.md` | Day6 答辩 PPT 大纲 |

## 验证

```bash
./mvnw test
./mvnw -Dmaven.compiler.showWarnings=true -Dmaven.compiler.compilerArgs=-Xlint:all test-compile
git diff --check
```
