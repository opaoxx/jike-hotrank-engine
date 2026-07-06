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

import java.util.function.Supplier;

@Slf4j
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;
    private final RankingCacheManager cacheManager;

    @GetMapping("/global")
    public ApiResponse<RankingResponseDTO> getGlobalRanking(@RequestParam(required = false) Integer limit) {
        log.info("Query global ranking: limit={}", limit);
        String cacheKey = RankingCacheManager.globalRankKey(limit);
        return getCachedRanking(cacheKey, () -> rankingService.getGlobalRanking(limit));
    }

    @GetMapping("/circle/{circleId}")
    public ApiResponse<RankingResponseDTO> getCircleRanking(
            @PathVariable Long circleId,
            @RequestParam(required = false) Integer limit) {
        log.info("Query circle ranking: circleId={}, limit={}", circleId, limit);

        if (circleId == null || circleId <= 0) {
            return ApiResponse.error(400, "圈子ID无效");
        }

        String cacheKey = RankingCacheManager.circleRankKey(circleId, limit);
        return getCachedRanking(cacheKey, () -> rankingService.getCircleRanking(circleId, limit));
    }

    @GetMapping("/newcomer")
    public ApiResponse<RankingResponseDTO> getNewcomerRanking(@RequestParam(required = false) Integer limit) {
        log.info("Query newcomer ranking: limit={}", limit);
        String cacheKey = RankingCacheManager.newcomerRankKey(limit);
        return getCachedRanking(cacheKey, () -> rankingService.getNewcomerRanking(limit));
    }

    @GetMapping("/surging")
    public ApiResponse<RankingResponseDTO> getSurgingRanking(@RequestParam(required = false) Integer limit) {
        log.info("Query surging ranking: limit={}", limit);
        String cacheKey = RankingCacheManager.surgingRankKey(limit);
        return getCachedRanking(cacheKey, () -> rankingService.getSurgingRanking(limit));
    }

    @GetMapping("/personalized")
    public ApiResponse<RankingResponseDTO> getPersonalizedRanking(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer limit) {
        log.info("Query personalized ranking: userId={}, limit={}", userId, limit);

        if (userId == null || userId <= 0) {
            return ApiResponse.error(400, "用户ID无效");
        }

        String cacheKey = RankingCacheManager.personalizedRankKey(userId, limit);
        return getCachedRanking(cacheKey, () -> rankingService.getPersonalizedGlobalRanking(userId, limit));
    }

    private ApiResponse<RankingResponseDTO> getCachedRanking(String cacheKey, Supplier<RankingResponseDTO> loader) {
        RankingCacheManager.CacheResult cached = cacheManager.getResult(cacheKey);
        if (cached.hit()) {
            return ApiResponse.success(cached.data());
        }

        RankingResponseDTO response = loader.get();
        if (response != null) {
            cacheManager.put(cacheKey, response);
        } else {
            cacheManager.putNull(cacheKey);
        }
        return ApiResponse.success(response);
    }
}
