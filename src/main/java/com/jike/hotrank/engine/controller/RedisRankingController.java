package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.cache.RedisRankingService;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.dto.RankingResponseDTO;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Redis ZSet 排名接口（与 MySQL 方案对比用）。
 * <p>
 * 端点前缀 /api/redis-ranking 与 /api/ranking 并列，
 * 可在压测时直接对比两套方案的 QPS 和响应时间。
 *
 * @author JikeHotRank Team
 */
@Slf4j
@RestController
@RequestMapping("/api/redis-ranking")
@RequiredArgsConstructor
public class RedisRankingController {

    private final RedisRankingService redisRankingService;
    private final TopicService topicService;

    @GetMapping("/global")
    public ApiResponse<RankingResponseDTO> getGlobalRanking(@RequestParam(required = false) Integer limit) {
        return ApiResponse.success(redisRankingService.getGlobalRanking(limit != null ? limit : 50));
    }

    @GetMapping("/circle/{circleId}")
    public ApiResponse<RankingResponseDTO> getCircleRanking(
            @PathVariable Long circleId,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.success(redisRankingService.getCircleRanking(circleId, limit != null ? limit : 20));
    }

    /**
     * 手动触发全量同步（将 MySQL 中所有话题分数写入 Redis ZSet）。
     * 生产环境应由定时任务自动调用，这里提供手动入口方便演示。
     */
    @PostMapping("/sync")
    public ApiResponse<String> syncAll() {
        List<Topic> topics = topicService.listAll();
        redisRankingService.syncAllTopics(topics);
        return ApiResponse.success("已同步 " + topics.size() + " 个话题到 Redis");
    }

    /**
     * 清空 Redis 排名数据。
     */
    @PostMapping("/flush")
    public ApiResponse<String> flush() {
        redisRankingService.flushAll();
        return ApiResponse.success("Redis 排名数据已清空");
    }
}
