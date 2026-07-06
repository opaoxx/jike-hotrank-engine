package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.dto.RankingResponseDTO;
import com.jike.hotrank.engine.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 榜单查询控制器
 * 提供四类榜单查询接口：全站热榜、圈子热榜、新星榜、飙升榜
 * 支持个性化榜单
 * <p>
 * 集成JVM本地缓存，5秒TTL
 *
 * @author JikeHotRank Team
 */
@Slf4j
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;
    private final RankingCacheManager cacheManager;

    /**
     * 查询全站热榜
     * <p>
     * 返回全局TOP50话题，按热度分降序排列
     *
     * @param limit 返回数量限制（可选，默认50）
     * @return 全站热榜
     */
    @GetMapping("/global")
    public ApiResponse<RankingResponseDTO> getGlobalRanking(
            @RequestParam(required = false) Integer limit) {
        log.info("查询全站热榜：limit={}", limit);

        // 尝试从缓存获取
        String cacheKey = RankingCacheManager.globalRankKey(limit);
        RankingResponseDTO cached = cacheManager.get(cacheKey);
        if (cached != null) {
            return ApiResponse.success(cached);
        }

        // 缓存未命中，查询数据库
        RankingResponseDTO response = rankingService.getGlobalRanking(limit);

        // 写入缓存
        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            cacheManager.put(cacheKey, response);
        } else {
            cacheManager.putNull(cacheKey);
        }

        return ApiResponse.success(response);
    }

    /**
     * 查询圈子热榜
     * <p>
     * 返回指定圈子的TOP20话题，按热度分降序排列
     *
     * @param circleId 圈子ID
     * @param limit 返回数量限制（可选，默认20）
     * @return 圈子热榜
     */
    @GetMapping("/circle/{circleId}")
    public ApiResponse<RankingResponseDTO> getCircleRanking(
            @PathVariable Long circleId,
            @RequestParam(required = false) Integer limit) {
        log.info("查询圈子热榜：circleId={}, limit={}", circleId, limit);

        if (circleId == null || circleId <= 0) {
            return ApiResponse.error(400, "圈子ID无效");
        }

        // 尝试从缓存获取
        String cacheKey = RankingCacheManager.circleRankKey(circleId, limit);
        RankingResponseDTO cached = cacheManager.get(cacheKey);
        if (cached != null) {
            return ApiResponse.success(cached);
        }

        // 缓存未命中，查询数据库
        RankingResponseDTO response = rankingService.getCircleRanking(circleId, limit);

        // 写入缓存
        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            cacheManager.put(cacheKey, response);
        } else {
            cacheManager.putNull(cacheKey);
        }

        return ApiResponse.success(response);
    }

    /**
     * 查询新星榜
     * <p>
     * 返回24小时内发布的热门话题TOP10
     *
     * @param limit 返回数量限制（可选，默认10）
     * @return 新星榜
     */
    @GetMapping("/newcomer")
    public ApiResponse<RankingResponseDTO> getNewcomerRanking(
            @RequestParam(required = false) Integer limit) {
        log.info("查询新星榜：limit={}", limit);

        // 尝试从缓存获取
        String cacheKey = RankingCacheManager.newcomerRankKey(limit);
        RankingResponseDTO cached = cacheManager.get(cacheKey);
        if (cached != null) {
            return ApiResponse.success(cached);
        }

        // 缓存未命中，查询数据库
        RankingResponseDTO response = rankingService.getNewcomerRanking(limit);

        // 写入缓存
        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            cacheManager.put(cacheKey, response);
        } else {
            cacheManager.putNull(cacheKey);
        }

        return ApiResponse.success(response);
    }

    /**
     * 查询飙升榜
     * <p>
     * 返回近1小时热度增速最快的话题TOP10
     *
     * @param limit 返回数量限制（可选，默认10）
     * @return 飙升榜
     */
    @GetMapping("/surging")
    public ApiResponse<RankingResponseDTO> getSurgingRanking(
            @RequestParam(required = false) Integer limit) {
        log.info("查询飙升榜：limit={}", limit);

        // 尝试从缓存获取
        String cacheKey = RankingCacheManager.surgingRankKey(limit);
        RankingResponseDTO cached = cacheManager.get(cacheKey);
        if (cached != null) {
            return ApiResponse.success(cached);
        }

        // 缓存未命中，查询数据库
        RankingResponseDTO response = rankingService.getSurgingRanking(limit);

        // 写入缓存
        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            cacheManager.put(cacheKey, response);
        } else {
            cacheManager.putNull(cacheKey);
        }

        return ApiResponse.success(response);
    }

    /**
     * 查询个性化热榜
     * <p>
     * 根据用户的圈子偏好权重对榜单进行重排
     *
     * @param userId 用户ID
     * @param limit 返回数量限制（可选，默认50）
     * @return 个性化热榜
     */
    @GetMapping("/personalized")
    public ApiResponse<RankingResponseDTO> getPersonalizedRanking(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer limit) {
        log.info("查询个性化热榜：userId={}, limit={}", userId, limit);

        if (userId == null || userId <= 0) {
            return ApiResponse.error(400, "用户ID无效");
        }

        // 个性化榜单不缓存（每个用户不同）
        RankingResponseDTO response = rankingService.getPersonalizedGlobalRanking(userId, limit);
        return ApiResponse.success(response);
    }
}
