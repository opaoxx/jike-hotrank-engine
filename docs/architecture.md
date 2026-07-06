# 即刻App内容社区实时热点榜单引擎 - 系统架构文档

## 1. 项目概述

### 1.1 项目背景
即刻App热点榜单引擎是一个模拟兴趣社区平台（即刻/小红书）的热点榜单核心系统。用户每次点赞、评论、转发都会实时影响话题热度，系统需要在分钟级内将热度变化反映到榜单上。

### 1.2 核心功能
- 实时互动事件采集
- 热度聚合计算（5分钟定时任务）
- 多维度榜单查询（全站/圈子/新星/飙升/个性化）
- 防刷热度机制
- JVM本地缓存
- 话题屏蔽
- 上榜事件触发

### 1.3 技术栈
| 组件 | 技术选型 |
|------|----------|
| 框架 | Spring Boot 4.0.6 |
| ORM | MyBatis 4.0.1 |
| 数据库 | MySQL 8.0 |
| 缓存 | JVM本地缓存（ConcurrentHashMap） |
| 定时任务 | Spring @Scheduled |
| 工具 | Lombok |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端 (Apifox/App)                       │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTP
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Controller 层                          │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐     │   │
│  │  │ Interaction  │ │   Ranking    │ │    Topic     │     │   │
│  │  │  Controller  │ │  Controller  │ │  Controller  │     │   │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘     │   │
│  └─────────┼────────────────┼────────────────┼─────────────┘   │
│            │                │                │                  │
│  ┌─────────┼────────────────┼────────────────┼─────────────┐   │
│  │         ▼        Service 层              ▼               │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐     │   │
│  │  │ Interaction  │ │   Ranking    │ │    Topic     │     │   │
│  │  │   Service    │ │   Service    │ │   Service    │     │   │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘     │   │
│  │         │                │                │              │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐     │   │
│  │  │  AntiSpam    │ │    User      │ │    User      │     │   │
│  │  │   Service    │ │  Behavior    │ │  Preference  │     │   │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘     │   │
│  └─────────┼────────────────┼────────────────┼─────────────┘   │
│            │                │                │                  │
│  ┌─────────┼────────────────┼────────────────┼─────────────┐   │
│  │         ▼        Mapper 层               ▼               │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐     │   │
│  │  │ Interaction  │ │    Topic     │ │   Circle     │     │   │
│  │  │   Mapper     │ │   Mapper     │ │   Mapper     │     │   │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘     │   │
│  └─────────┼────────────────┼────────────────┼─────────────┘   │
│            │                │                │                  │
│  ┌─────────┼────────────────┼────────────────┼─────────────┐   │
│  │         ▼        定时任务层               ▼               │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐     │   │
│  │  │    Heat      │ │   Snapshot   │ │   Ranking    │     │   │
│  │  │ Aggregation  │ │    Task      │ │    Cache     │     │   │
│  │  │    Task      │ │              │ │   Manager    │     │   │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘     │   │
│  └─────────┼────────────────┼────────────────┼─────────────┘   │
└────────────┼────────────────┼────────────────┼─────────────────┘
             │                │                │
             ▼                ▼                ▼
┌─────────────────────────────────────────────────────────────────┐
│                         MySQL 8.0 数据库                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │  topic   │ │interaction│ │  circle  │ │snapshot  │           │
│  │          │ │  _event   │ │          │ │          │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 数据流向

```
用户互动 → Controller → AntiSpam校验 → 记录事件 → 更新用户偏好
                                    ↓
                            interaction_event表
                                    ↓
                        HeatAggregationTask (每5分钟)
                                    ↓
                        计算时间衰减热度分
                                    ↓
                        更新 topic.current_score
                                    ↓
                        榜单查询 ← RankingCacheManager (5秒TTL)
```

---

## 3. 核心算法

### 3.1 热度算法（时间衰减模型）

**公式：**
```
score = 总互动分 / (发布小时数 + 2)^1.8
```

**参数说明：**
- `总互动分` = 点赞数×1 + 收藏数×2 + 转发数×3 + 评论数×5
- `发布小时数` = 当前时间 - 发布时间（小时）
- `1.8` = 时间衰减指数（介于线性1.0和平方2.0之间）
- `+2` = 时间偏移量（防止新话题分数过高）

**互动权重：**
| 互动类型 | 权重 | 说明 |
|----------|------|------|
| 评论 | 5 | 最高价值互动 |
| 转发 | 3 | 传播价值 |
| 收藏 | 2 | 收藏价值 |
| 点赞 | 1 | 基础互动 |

### 3.2 个性化榜单算法

**公式：**
```
个性化分数 = 原始热度分 × 用户圈子偏好权重
```

**偏好权重计算：**
```
weight = 1 + log2(interactionCount)  (最大10.0)
```

---

## 4. 防刷机制

### 4.1 滑动窗口频率限制
- 同一用户对同一话题24小时内有效互动上限：10次
- 超出不计入热度但记录日志

### 4.2 设备指纹聚合检测
- 相同设备指纹的多个账号互动不叠加计算
- 阈值：同一设备指纹关联用户数 ≥ 5 视为异常

### 4.3 异常突增检测
- 某话题1小时内互动量超过历史均值10倍
- 自动触发待审核标记，暂停计入热榜

---

## 5. 缓存策略

### 5.1 JVM本地缓存
- 实现：ConcurrentHashMap + 定时清理
- TTL：5秒
- 随机化过期：±500ms（防止缓存雪崩）

### 5.2 缓存防护
- **缓存穿透**：空值缓存（TTL 2.5秒）
- **缓存雪崩**：随机化TTL分散过期时间

### 5.3 缓存失效
- 话题屏蔽后立即清除所有榜单缓存
- 话题恢复后立即清除所有榜单缓存

---

## 6. 定时任务

| 任务 | 频率 | 说明 |
|------|------|------|
| HeatAggregationTask | 每5分钟 | 聚合互动事件，计算热度分 |
| SnapshotTask | 每小时整点 | 保存TOP100快照，支持历史回溯 |

---

## 7. 数据库设计

### 7.1 ER图

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   circle    │       │    topic    │       │ interaction │
│─────────────│       │─────────────│       │    _event   │
│ id (PK)     │◄──┐   │ id (PK)     │◄──┐   │─────────────│
│ name        │   └───│ circle_id   │   │   │ id (PK)     │
│ description │       │ title       │   └───│ topic_id    │
│ status      │       │ content     │       │ user_id     │
│ sort_order  │       │ author_id   │       │ interaction │
└─────────────┘       │ publish_time│       │   _type     │
                      │ current_    │       │ device_     │
                      │   score     │       │ fingerprint │
                      │ interaction │       └─────────────┘
                      │   _count    │
                      │ status      │       ┌─────────────┐
                      └─────────────┘       │   user_     │
                                            │  behavior   │
┌─────────────┐       ┌─────────────┐       │─────────────│
│ user_circle │       │   topic_    │       │ id (PK)     │
│ _preference │       │   score_    │       │ user_id     │
│─────────────│       │  snapshot   │       │ topic_id    │
│ id (PK)     │       │─────────────│       │ interaction │
│ user_id     │       │ id (PK)     │       │   _type     │
│ circle_id   │       │ topic_id    │       │ is_valid    │
│ weight      │       │ circle_id   │       │ invalid_    │
│ interaction │       │ score       │       │   reason    │
│   _count    │       │ rank_position│      └─────────────┘
└─────────────┘       │ snapshot_time│
                      └─────────────┘
```

### 7.2 索引设计

| 表名 | 索引 | 用途 |
|------|------|------|
| topic | idx_circle_score | 圈子热榜查询 |
| topic | idx_publish_time | 新星榜查询 |
| interaction_event | idx_topic_created | 互动聚合查询 |
| interaction_event | idx_user_topic | 频率限制检查 |
| topic_score_snapshot | idx_snapshot_time | 历史快照查询 |

---

## 8. API接口清单

### 8.1 互动事件接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/interaction | 记录互动事件 |

### 8.2 榜单查询接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/ranking/global | 全站热榜 |
| GET | /api/ranking/circle/{circleId} | 圈子热榜 |
| GET | /api/ranking/newcomer | 新星榜 |
| GET | /api/ranking/surging | 飙升榜 |
| GET | /api/ranking/personalized | 个性化热榜 |

### 8.3 话题管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/topic/{id} | 查询话题详情 |
| POST | /api/topic/{id}/block | 屏蔽话题 |
| POST | /api/topic/{id}/unblock | 恢复话题 |

### 8.4 防刷报告接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/anti-spam/report | 防刷检测报告 |

---

## 9. 项目结构

```
src/main/java/com/jike/hotrank/engine/
├── JikeHotrankEngineApplication.java      # 启动类
├── cache/
│   └── RankingCacheManager.java           # JVM缓存管理
├── controller/
│   ├── AntiSpamController.java            # 防刷报告接口
│   ├── InteractionController.java         # 互动事件接口
│   ├── RankingController.java             # 榜单查询接口
│   └── TopicController.java               # 话题管理接口
├── dto/
│   ├── ApiResponse.java                   # 统一响应体
│   ├── RankingItemDTO.java                # 排名项DTO
│   └── RankingResponseDTO.java            # 榜单响应DTO
├── entity/                                # 实体类
├── exception/
│   ├── BusinessException.java             # 业务异常
│   └── GlobalExceptionHandler.java        # 全局异常处理
├── mapper/                                # Mapper接口
├── service/
│   ├── AntiSpamService.java               # 防刷服务
│   ├── AntiSpamReportService.java         # 防刷报告服务
│   ├── CircleService.java                 # 圈子服务
│   ├── InteractionEventService.java       # 互动事件服务
│   ├── RankingService.java                # 榜单服务
│   ├── TopicService.java                  # 话题服务
│   ├── UserBehaviorService.java           # 用户行为服务
│   └── UserCirclePreferenceService.java   # 用户偏好服务
├── task/
│   ├── HeatAggregationTask.java           # 热度聚合任务
│   └── SnapshotTask.java                  # 快照任务
└── util/
    └── HeatScoreCalculator.java           # 热度算法
```
