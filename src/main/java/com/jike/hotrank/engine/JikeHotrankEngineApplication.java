package com.jike.hotrank.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

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

    private static final Logger log = LoggerFactory.getLogger(JikeHotrankEngineApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JikeHotrankEngineApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String banner = """

            ============================================================
              Jike HotRank Engine started successfully!

              Local URL:       http://localhost:8080
              API Base:        http://localhost:8080/api
              Dashboard:       http://localhost:8080/index.html
              SSE Stream:      http://localhost:8080/api/notifications/rankings/stream
              ---
              Key endpoints:
                GET  /api/ranking/global?limit=50        - Global hot ranking
                GET  /api/ranking/circle/{id}?limit=20   - Circle ranking
                GET  /api/ranking/newcomer?limit=10      - Newcomer ranking
                GET  /api/ranking/surging?limit=10       - Surging ranking
                POST /api/interaction                     - Record interaction
                GET  /api/analysis/overview               - Analysis overview
                GET  /api/anti-spam/report                - Anti-spam report
                GET  /api/notifications/rankings/stream   - SSE real-time events
            ============================================================
            """;
        log.info(banner);
    }
}
