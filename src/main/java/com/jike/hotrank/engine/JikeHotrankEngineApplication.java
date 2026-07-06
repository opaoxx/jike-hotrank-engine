package com.jike.hotrank.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 即刻App内容社区实时热点榜单引擎 - 启动类
 * <p>
 * 核心功能：
 * 1. 实时互动事件采集
 * 2. 热度聚合计算（5分钟定时任务）
 * 3. 多维度榜单查询（全站/圈子/新星/飙升）
 * 4. 防刷热度机制
 * 5. JVM本地缓存
 *
 * @author JikeHotRank Team
 */
@SpringBootApplication
public class JikeHotrankEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(JikeHotrankEngineApplication.class, args);
    }
}
