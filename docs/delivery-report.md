# 最终交付报告

本文档用于项目提交前核对 7 天实训目标、代码实现、文档和验证证据。

## 交付概览

| 类别 | 交付物 |
| --- | --- |
| 后端服务 | Spring Boot + MyBatis + MySQL 热点榜单引擎 |
| 核心接口 | 互动写入、全站榜单、圈子榜单、新星榜、飙升榜、个性化榜单 |
| 反作弊 | 频率限制、设备指纹降权、异常突增检测、审计报告 |
| 缓存与推送 | 本地榜单缓存、空值缓存、随机 TTL、SSE 榜单事件 |
| 运维工具 | 手动聚合、手动快照、修复 SQL、压测脚本、EXPLAIN 清单 |
| 文档 | README、架构文档、数据库设计、性能分析、Demo 手册、答辩手册 |

## 7 天任务覆盖

| 天数 | 原目标 | 当前证据 |
| --- | --- | --- |
| Day1 | 业务建模、数据库建模、热度算法选型 | `schema.sql`、`docs/database-design.md`、`HeatScoreCalculator` |
| Day2 | 互动事件采集、热度聚合、多维榜单、快照 | `InteractionController`、`HeatAggregationTask`、`RankingService`、`SnapshotTask` |
| Day3 | 防刷机制、查询缓存、性能对比 | `AntiSpamService`、`RankingCacheManager`、`AntiSpamReportService` |
| Day4 | 个性化榜单、实时推送、话题屏蔽、遗留重构 | `UserCirclePreferenceService`、`RankingNotificationService`、`TopicController`、`TaskLockService` |
| Day5 | 全链路压测、数据分析、索引审查 | `LoadTestService`、`AnalysisService`、`docs/performance-analysis.md`、`docs/sql-explain-checklist.md` |
| Day6 | 架构文档、数据库文档、Demo、互评、PPT | `docs/architecture.md`、`docs/demo-guide.md`、`docs/final-review-checklist.md`、`docs/presentation-outline.md` |
| Day7 | 项目答辩、追问准备、复盘与演进 | `docs/day7-defense-guide.md`、本交付报告 |

## API 验收矩阵

| 场景 | 接口 |
| --- | --- |
| 写入互动 | `POST /api/interaction` |
| 全站热榜 | `GET /api/ranking/global?limit=50` |
| 圈子热榜 | `GET /api/ranking/circle/{circleId}?limit=20` |
| 新星榜 | `GET /api/ranking/newcomer?limit=10` |
| 飙升榜 | `GET /api/ranking/surging?limit=10` |
| 个性化榜单 | `GET /api/ranking/personalized?userId={userId}&limit=50` |
| 话题屏蔽 | `POST /api/topic/{id}/block` |
| 话题恢复 | `POST /api/topic/{id}/unblock` |
| 防刷报告 | `GET /api/anti-spam/report` |
| 实时推送 | `GET /api/notifications/rankings/stream` |
| 压测入口 | `POST /api/perf/load-test?qps=20&duration=5&token=perf_test_token` |
| 缓存统计 | `GET /api/perf/cache-comparison` |
| 数据分析总览 | `GET /api/analysis/overview` |
| 手动聚合 | `POST /api/ops/heat-aggregation?token=ops_demo_token` |
| 手动快照 | `POST /api/ops/snapshot?token=ops_demo_token` |

## 质量证据

提交前建议执行：

```bash
./mvnw test
./mvnw -Dmaven.compiler.showWarnings=true -Dmaven.compiler.compilerArgs=-Xlint:all test-compile
git diff --check
```

补充扫描：

- 待办标记、通配符导入、控制台直写、直接打印异常栈。
- README、docs、SQL 和测试文件中的常见乱码标记。
- SQL 脚本和迁移脚本中的关键索引一致性。

上述后两类扫描已通过 `SqlScriptConsistencyTest` 覆盖一部分，提交前仍建议人工复核命令输出。

## 已知限制

- 当前缓存为 JVM 本地缓存，多实例下无法天然共享缓存内容。
- 运维触发入口使用简单 token，适合课程 Demo，不等同于生产权限体系。
- 压测接口是轻量模拟器，真实 1000 QPS 验证应使用独立压测工具和隔离环境。
- 热度聚合当前按全量互动重新计算，数据量扩大后需要窗口化、增量化或流式聚合。
- 个性化榜单采用候选集重排，超大用户规模下需要离线画像和召回层。

## 后续演进路线

| 阶段 | 目标 | 方案 |
| --- | --- | --- |
| 短期 | 提升多实例读一致性 | Redis 缓存和发布订阅失效 |
| 中期 | 承接互动写入峰值 | Kafka 或 RocketMQ 削峰填谷 |
| 中期 | 提升榜单实时性 | Redis ZSet 维护实时排名 |
| 长期 | 支持数据分析和推荐 | 数仓、离线特征、用户画像 |
| 长期 | 完善运营后台 | 权限体系、审核流、配置中心 |
