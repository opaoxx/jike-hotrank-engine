# 即刻App内容社区实时热点榜单引擎

> JikeHotRankEngine - 实时热点榜单核心系统

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-green.svg)](https://spring.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![MyBatis](https://img.shields.io/badge/MyBatis-4.0.1-orange.svg)](https://mybatis.org/)
[![Java](https://img.shields.io/badge/Java-21-red.svg)](https://www.oracle.com/java/)

---

## 📖 项目简介

模拟兴趣社区平台（即刻/小红书）的热点榜单核心系统。用户每次点赞、评论、转发都会实时影响话题热度，系统在分钟级内将热度变化反映到榜单上。

### 核心特性

- ✅ **实时互动事件采集** - 支持点赞/收藏/转发/评论四种互动类型
- ✅ **热度聚合计算** - 每5分钟定时聚合，使用时间衰减算法
- ✅ **多维度榜单** - 全站热榜、圈子热榜、新星榜、飙升榜、个性化榜单
- ✅ **防刷机制** - 频率限制、设备指纹检测、异常突增检测
- ✅ **JVM本地缓存** - 5秒TTL，防缓存穿透/雪崩
- ✅ **话题屏蔽** - 屏蔽后立即从榜单消失
- ✅ **上榜事件触发** - 话题首次进入TOP10时触发通知

---

## 🛠️ 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 4.0.6 |
| ORM | MyBatis | 4.0.1 |
| 数据库 | MySQL | 8.0 |
| 缓存 | JVM本地缓存 | - |
| 工具 | Lombok | - |
| Java | JDK | 21 |

---

## 📁 项目结构

```
jike-hotrank-engine/
├── src/
│   ├── main/
│   │   ├── java/com/jike/hotrank/engine/
│   │   │   ├── JikeHotrankEngineApplication.java    # 启动类
│   │   │   ├── cache/                                # 缓存层
│   │   │   ├── controller/                           # 控制器层
│   │   │   ├── dto/                                  # 数据传输对象
│   │   │   ├── entity/                               # 实体类
│   │   │   ├── exception/                            # 异常处理
│   │   │   ├── mapper/                               # Mapper接口
│   │   │   ├── service/                              # 服务层
│   │   │   ├── task/                                 # 定时任务
│   │   │   └── util/                                 # 工具类
│   │   └── resources/
│   │       ├── application.properties                # 配置文件
│   │       ├── mapper/                               # MyBatis XML
│   │       └── sql/                                  # SQL脚本
│   └── test/                                         # 测试代码
├── docs/                                             # 文档
│   ├── architecture.md                               # 架构文档
│   └── loadtest/                                     # 压测脚本
├── pom.xml                                           # Maven配置
└── README.md                                         # 项目说明
```

---

## 🚀 快速开始

### 环境要求

- JDK 21+
- MySQL 8.0+
- Maven 3.6+

### 1. 克隆项目

```bash
git clone <repository-url>
cd jike-hotrank-engine
```

### 2. 创建数据库

```bash
# 执行建表脚本
mysql -u root -p < src/main/resources/sql/schema.sql

# 导入测试数据
mysql -u root -p < src/main/resources/sql/data.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.properties`：

```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

或在IDE中运行 `JikeHotrankEngineApplication.main()`

### 5. 访问应用

应用启动后访问：`http://localhost:8080`

---

## 📡 API接口

### 互动事件接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/interaction` | 记录互动事件 |

**请求示例：**
```json
{
    "topicId": 1,
    "userId": 9001,
    "interactionType": 1,
    "deviceFingerprint": "device_001",
    "ipAddress": "192.168.1.100"
}
```

### 榜单查询接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ranking/global` | 全站热榜 |
| GET | `/api/ranking/circle/{circleId}` | 圈子热榜 |
| GET | `/api/ranking/newcomer` | 新星榜 |
| GET | `/api/ranking/surging` | 飙升榜 |
| GET | `/api/ranking/personalized?userId={userId}` | 个性化热榜 |

### 话题管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/topic/{id}` | 查询话题详情 |
| POST | `/api/topic/{id}/block` | 屏蔽话题 |
| POST | `/api/topic/{id}/unblock` | 恢复话题 |

### 防刷报告接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/anti-spam/report` | 防刷检测报告 |

---

## 🔥 热度算法

### 时间衰减模型（Hacker News算法变种）

```
score = 总互动分 / (发布小时数 + 2)^1.8
```

### 互动权重

| 互动类型 | 权重 | 说明 |
|----------|------|------|
| 评论 | 5 | 最高价值互动 |
| 转发 | 3 | 传播价值 |
| 收藏 | 2 | 收藏价值 |
| 点赞 | 1 | 基础互动 |

---

## 🛡️ 防刷机制

1. **滑动窗口频率限制** - 同一用户对同一话题24小时内互动上限10次
2. **设备指纹聚合检测** - 相同设备指纹多账号不叠加计算
3. **异常突增检测** - 1小时内互动量超历史均值10倍触发审核

---

## 💾 缓存策略

- **缓存类型**：JVM本地缓存（ConcurrentHashMap）
- **TTL**：5秒
- **防缓存穿透**：空值缓存
- **防缓存雪崩**：随机化TTL（±500ms）

---

## ⏰ 定时任务

| 任务 | 频率 | 说明 |
|------|------|------|
| 热度聚合任务 | 每5分钟 | 聚合互动事件，计算热度分 |
| 快照任务 | 每小时 | 保存TOP100快照，支持历史回溯 |

---

## 📊 数据库设计

### 核心表

| 表名 | 说明 |
|------|------|
| circle | 圈子表 |
| topic | 话题表 |
| interaction_event | 互动事件表 |
| user_behavior | 用户行为表 |
| topic_score_snapshot | 热度快照表 |
| user_circle_preference | 用户圈子偏好表 |

---

## 📝 开发文档

- [系统架构文档](docs/architecture.md)
- [压测脚本](docs/loadtest/)

---

## 👥 开发团队

JikeHotRank Team

---

## 📄 许可证

本项目仅供学习使用
