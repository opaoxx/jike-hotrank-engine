# Day5 性能压测与数据分析

## 目标

Day5 关注两件事：

- 用可重复的压测入口观察接口吞吐、成功率和延迟。
- 用只读分析接口观察热度分布、互动行为、圈子活跃度、反作弊效果和缓存命中率。

## 压测入口

外部脚本：

```bash
BASE_URL=http://localhost:8080 REQUESTS=200 docs/loadtest/benchmark.sh
```

Windows：

```bat
set BASE_URL=http://localhost:8080
set REQUESTS=200
docs\loadtest\benchmark.bat
```

脚本会将原始结果写入 `docs/loadtest/results/benchmark-*.csv`。该目录已加入 `.gitignore`，避免压测产物进入仓库。

内置轻量压测接口：

```bash
curl -X POST "http://localhost:8080/api/perf/load-test?qps=20&duration=5&token=perf_test_token"
```

默认安全上限：

- `qps`: 1~200
- `duration`: 1~30 秒
- `token`: `jike-hotrank.performance.token`

## 分析接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/analysis/heat-distribution` | 热度分数区间分布、平均值、中位数、最高分话题 |
| GET | `/api/analysis/interaction-stats?hours=24` | 最近 N 小时互动类型占比 |
| GET | `/api/analysis/circle-activity` | 圈子话题数、平均热度、互动总数、活跃排名 |
| GET | `/api/analysis/anti-cheat-stats?days=7` | 无效行为原因分布、日趋势、TOP 拦截话题 |
| GET | `/api/analysis/overview` | 上述分析与缓存统计总览 |
| GET | `/api/perf/cache-comparison` | 当前进程内榜单缓存统计 |

## 验收建议

- 先执行 `schema.sql` 和 `data.sql` 初始化数据。
- 启动应用后先访问一次全站榜，再访问 `/api/perf/cache-comparison` 观察 miss。
- 连续访问全站榜数次，再观察 hitRate 是否上升。
- 执行外部压测脚本，检查 CSV 中非 2xx 状态码占比。
- 执行内置压测接口，检查 `successRate`、`actualQps`、`latency.p95Ms`、`latency.p99Ms`。
- 使用 `docs/sql-explain-checklist.md` 中的 SQL 检查关键查询是否命中预期索引。
