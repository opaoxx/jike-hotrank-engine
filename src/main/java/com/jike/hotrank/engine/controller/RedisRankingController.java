package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.cache.RedisRankingService;
import com.jike.hotrank.engine.config.HotRankProperties;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.dto.RankingResponseDTO;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.TopicService;
import com.jike.hotrank.engine.util.RankingLimits;
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
    private final HotRankProperties properties;

    @GetMapping("/global")
    public ApiResponse<RankingResponseDTO> getGlobalRanking(@RequestParam(required = false) Integer limit) {
        int actualLimit = RankingLimits.global(limit);
        return ApiResponse.success(redisRankingService.getGlobalRanking(actualLimit));
    }

    @GetMapping("/circle/{circleId}")
    public ApiResponse<RankingResponseDTO> getCircleRanking(
            @PathVariable Long circleId,
            @RequestParam(required = false) Integer limit) {
        int actualLimit = RankingLimits.circle(limit);
        return ApiResponse.success(redisRankingService.getCircleRanking(circleId, actualLimit));
    }

    /**
     * 手动触发全量同步（将 MySQL 中所有话题分数写入 Redis ZSet）。
     * 生产环境应由定时任务自动调用，这里提供手动入口方便演示。
     */
    @PostMapping("/sync")
    public ApiResponse<String> syncAll(@RequestParam String token) {
        if (!isValidToken(token)) {
            return ApiResponse.error(403, "运维 token 无效");
        }
        List<Topic> topics = topicService.listAll();
        redisRankingService.syncAllTopics(topics);
        return ApiResponse.success("已同步 " + topics.size() + " 个话题到 Redis");
    }

    /**
     * 清空 Redis 排名数据。
     */
    @PostMapping("/flush")
    public ApiResponse<String> flush(@RequestParam String token) {
        if (!isValidToken(token)) {
            return ApiResponse.error(403, "运维 token 无效");
        }
        redisRankingService.flushAll();
        return ApiResponse.success("Redis 排名数据已清空");
    }

    private boolean isValidToken(String token) {
        return properties.getOperations().getToken().equals(token);
    }
}
