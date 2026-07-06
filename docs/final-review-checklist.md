# Day6 小组互评与终审清单

本文档用于 Day6 小组互评和提交前终审。结论以“通过 / 风险 / 待补”为准，不要求现场改动全部一次完成。

## 业务验收

| 检查项 | 当前实现证据 | 结论 |
| --- | --- | --- |
| 互动事件采集 | `POST /api/interaction` 写入 `interaction_event` 和 `user_behavior` | 通过 |
| 热度聚合 | `HeatAggregationTask` 按加权互动分和发布时间重算 `topic.current_score` | 通过 |
| 多维榜单 | 全站、圈子、新星、飙升、个性化榜单均有接口 | 通过 |
| 防刷机制 | 频率限制、设备指纹降权、异常突增检测 | 通过 |
| 查询缓存 | 本地缓存、空值缓存、随机 TTL、按前缀失效 | 通过 |
| 实时推送 | 聚合后发布 SSE 榜单更新和 TOPN 进入事件 | 通过 |
| 话题屏蔽 | 屏蔽/恢复接口更新状态并清理榜单缓存 | 通过 |
| 性能分析 | 压测入口、缓存统计、数据分析接口和 SQL EXPLAIN 清单 | 通过 |

## 代码互评重点

| 维度 | 检查问题 | 审查位置 |
| --- | --- | --- |
| 事务边界 | 互动写入、行为审计、偏好更新是否在明确事务内 | `InteractionWriteService` |
| 多实例任务 | 定时聚合和快照是否避免重复执行 | `TaskLockService`、`HeatAggregationTask`、`SnapshotTask` |
| 缓存一致性 | 热度聚合、话题屏蔽、个性化写入后是否清理对应缓存 | `RankingCacheManager` 及调用方 |
| 防刷漏洞 | 频率限制是否拒绝写入，设备风险是否降权，异常突增是否可追踪 | `AntiSpamService` |
| SQL 性能 | 榜单、聚合、分析和审计查询是否有匹配索引 | `schema.sql`、`docs/sql-explain-checklist.md` |
| 参数安全 | `limit`、`qps`、`duration`、token 参数是否有上限和鉴权 | Controller 层 |
| 可维护性 | 是否存在待办标记、通配符导入、控制台直写或直接打印异常栈 | 全仓库扫描 |

## 提交前验证命令

```bash
./mvnw test
./mvnw -Dmaven.compiler.showWarnings=true -Dmaven.compiler.compilerArgs=-Xlint:all test-compile
git diff --check
```

乱码扫描已内置在 `SqlScriptConsistencyTest.publicSqlAndDocsShouldNotContainCommonMojibakeMarkers` 中，随 `./mvnw test` 自动执行。
待办标记、通配符导入、控制台直写和直接打印异常栈等代码卫生项，提交前用 `rg` 在 `src`、`README.md`、`docs` 中统一扫描。

## 已知取舍

- 主榜单缓存为 JVM 本地缓存，适合单机课程项目；Redis ZSet 已作为对比排名通道接入，多实例主读一致性仍需要统一缓存失效或切主读链路。
- 压测入口默认限制在轻量范围，避免本地演示误触发高负载；真实 1000 QPS 应使用 JMeter 或 wrk 在独立环境执行。
- 手动运维触发入口仅用于 Demo 和本地验证，已通过 `jike-hotrank.operations.token` 做最小鉴权；生产环境应替换为正式后台权限体系。
- 个性化榜单先取全站候选再按圈子偏好重排，数据规模扩大后可演进为离线候选集或按用户分层的 Redis ZSet。

## Day7 答辩追问准备

| 问题 | 回答要点 |
| --- | --- |
| 为什么时间衰减指数取 1.8 | 参考 Hacker News 类时间衰减，让新内容有窗口期，同时避免旧内容长期霸榜；指数越大，新内容上升越快、旧内容下降越快 |
| 缓存和数据库不一致窗口多长 | 默认榜单缓存 TTL 为秒级，并在聚合、屏蔽、恢复时主动失效；业务接受短窗口换取读性能 |
| 1000 用户同时点赞能否扛住 | 当前单机 MySQL 可通过索引、批量聚合和缓存承受课程级压力；10 倍增长时瓶颈在写入流水和聚合，应引入 MQ 削峰、分库或读写分离 |
| 个性化榜单与全站榜单差异 | 全站榜单按全局热度排序；个性化榜单先取候选，再叠加用户圈子偏好权重，查询和缓存 key 都需要按用户隔离 |
| 防刷为什么不是全部拒绝 | 明确作弊如频率超限直接拒绝；设备风险这类弱信号采用降权，减少误伤真实用户 |
