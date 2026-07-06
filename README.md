# 即刻 App 内容社区实时热点榜单引擎

Jike HotRank Engine 是一个基于 Spring Boot、MyBatis 和 MySQL 的内容社区实时热点榜单引擎。项目围绕互动事件采集、热度聚合、多维榜单、反作弊、个性化排序、缓存和实时推送展开。

## 技术栈

| 模块 | 选型 |
| --- | --- |
| Java | JDK 21 |
| Web 框架 | Spring Boot 4.0.6 |
| ORM | MyBatis 4.0.1 |
| 数据库 | MySQL 8.0 |
| 缓存 | JVM 本地缓存 + Redis ZSet |
| 定时任务 | Spring Scheduling |
| 前端 | Vue 3 + Vite + Element Plus + ECharts |
| 测试 | JUnit 5 + Mockito |

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/00_setup_fresh_database.sql
```

已有数据库升级时：

```bash
mysql -u root -p --default-character-set=utf8mb4 < src/main/resources/sql/01_upgrade_existing_database.sql
```

更多 SQL 执行顺序见 `src/main/resources/sql/数据库脚本执行顺序说明.md`。

### 2. 启动后端

```bash
./mvnw spring-boot:run
```

默认配置位于 `src/main/resources/application.yml`：

```yaml
spring.datasource.url: jdbc:mysql://localhost:3306/jike_hotrank
spring.datasource.username: root
spring.datasource.password: root
```

启动后 API 地址：`http://localhost:8080`

### 3. 构建前端

```bash
cd frontend
npm install
npm run build
```

构建产物输出到 `src/main/resources/static/`，访问 `http://localhost:8080` 即可看到 Dashboard。

前端开发模式（热更新）：

```bash
cd frontend
npm run dev
```

访问 `http://localhost:5173`，API 请求自动代理到 `:8080`。

### 4. Redis（可选，用于对比压测）

项目同时实现了 MySQL 窗口函数排名和 Redis ZSet 排名两套方案，可在压测时直接对比 QPS 和延迟。

```bash
# WSL 或 Linux 中启动 Redis
sudo service redis-server start
redis-cli ping  # 返回 PONG 即正常
```

Redis 默认连接 `localhost:6379`，在 `application.yml` 中配置：

```yaml
spring.data.redis:
  host: localhost
  port: 6379
  timeout: 3000ms
```

> Windows 开发环境下，WSL2 的 Redis 通过 localhost 转发可直接被 Windows 上的 Java 应用访问，无需额外配置。

首次使用需手动同步数据：`POST /api/redis-ranking/sync?token=ops_demo_token`

## 项目结构

```
├── src/main/java/com/jike/hotrank/engine/
│   ├── controller/         # REST 接口（8 个 Controller）
│   ├── service/            # 业务逻辑
│   ├── mapper/             # MyBatis Mapper
│   ├── entity/             # 数据实体
│   ├── dto/                # 数据传输对象
│   ├── cache/              # JVM 缓存 + Redis 排名服务
│   ├── config/             # 配置类
│   ├── task/               # 定时任务（热度聚合、快照）
│   ├── util/               # 工具类（热度算法、限流）
│   └── exception/          # 全局异常处理
├── src/main/resources/
│   ├── mapper/             # MyBatis XML
│   ├── sql/                # 建表、数据、升级脚本
│   └── static/             # 前端构建产物（Vite 输出）
├── frontend/               # Vue 3 前端源码
│   ├── src/views/          # 6 个页面视图
│   ├── src/components/     # 通用组件 + ECharts 图表
│   ├── src/api/            # Axios API 封装
│   ├── src/composables/    # SSE、自动刷新
│   └── src/stores/         # Pinia 状态管理
└── docs/                   # 文档（API、架构、压测、答辩）
```

## Dashboard 前端

| Tab | 功能 |
|-----|------|
| 📊 总览 | 统计卡片 + 热度分布柱状图 + 互动饼图 + 圈子雷达图 + 缓存仪表盘 |
| 🌐 全站热榜 | 排名表格，点击行查看话题详情 / 屏蔽操作 |
| ⭕ 圈子热榜 | 选择圈子 → 对应榜单 |
| 🆕 新星榜 | 24h 内发布的话题排行 |
| 🚀 飙升榜 | 近 1h 增速最快的话题 |
| 🛡️ 反作弊 | 拦截趋势折线图 + 原因分布饼图 + 可疑用户列表 |

底部互动模拟器可直接提交互动事件，提交后自动刷新当前页面。

## 核心能力

- 记录点赞、收藏、转发、评论四类互动事件。
- 按互动权重、防刷倍率和发布时间衰减计算话题热度。
- 提供全站热榜、圈子热榜、新星榜、飙升榜和个性化热榜。
- 支持频率限制、设备指纹降权、异常突增审核标记。
- 使用本地缓存降低榜单查询压力，并缓存空结果避免穿透，随机 TTL 防止雪崩。
- 热度聚合后同步更新 Redis ZSet，支持 MySQL vs Redis 排名方案对比。
- 热度聚合后清理榜单缓存，并通过 SSE 推送榜单变化事件。
- 定时聚合与快照任务使用 MySQL named lock 避免多实例重复执行。
- 前端 SPA 路由回退 Filter，Vue Router History 模式与 Spring Boot 静态资源共存。

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

### Redis 排名（对比用）

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/redis-ranking/global?limit=50` | Redis 全站热榜 |
| GET | `/api/redis-ranking/circle/{circleId}?limit=20` | Redis 圈子热榜 |
| POST | `/api/redis-ranking/sync?token=ops_demo_token` | 手动全量同步到 Redis |
| POST | `/api/redis-ranking/flush?token=ops_demo_token` | 清空 Redis 排名数据 |

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
| POST | `/api/ops/heat-aggregation?token=ops_demo_token` | 手动触发热度聚合 |
| POST | `/api/ops/snapshot?token=ops_demo_token` | 手动生成榜单快照 |

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
| `src/main/resources/sql/数据库脚本执行顺序说明.md` | SQL 执行顺序说明 |
| `src/main/resources/sql/00_setup_fresh_database.sql` | 新库一键初始化入口 |
| `src/main/resources/sql/01_upgrade_existing_database.sql` | 旧库一键升级入口 |
| `src/main/resources/sql/schema.sql` | 初始化数据库表结构 |
| `src/main/resources/sql/data.sql` | 初始化本地演示数据 |
| `src/main/resources/sql/repair.sql` | 修复历史互动数、热度分、偏好权重和孤立数据 |
| `docs/loadtest/benchmark.bat` | Windows 压测入口 |
| `docs/loadtest/benchmark.sh` | Linux/macOS 压测入口 |
| `docs/接口参考文档.md` | API 参考文档 |
| `docs/系统架构说明.md` | 系统架构说明 |
| `docs/数据库设计说明.md` | 数据库设计说明 |
| `docs/热度算法对比报告.md` | Day1 热度算法对比报告 |
| `docs/窗口函数实践说明.md` | Day2 MySQL 窗口函数实践说明 |
| `docs/性能与数据分析说明.md` | 性能与数据分析说明 |
| `docs/数据库执行计划检查清单.md` | SQL EXPLAIN 检查清单 |
| `docs/演示手册.md` | Demo 演示手册 |
| `docs/最终检查清单.md` | Day6 互评与终审清单 |
| `docs/答辩演示大纲.md` | 答辩 PPT 大纲 |
| `docs/第七天答辩手册.md` | 答辩问答手册 |
| `docs/最终交付报告.md` | 最终交付报告 |

## 验证

```bash
./mvnw test
./mvnw -Dmaven.compiler.showWarnings=true -Dmaven.compiler.compilerArgs=-Xlint:all test-compile
cd frontend && npm run build
```
