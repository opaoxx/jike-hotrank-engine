package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.config.HotRankProperties;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.service.LoadTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/perf")
@RequiredArgsConstructor
public class PerfController {

    private final LoadTestService loadTestService;
    private final RankingCacheManager cacheManager;
    private final HotRankProperties properties;

    @PostMapping("/load-test")
    public ApiResponse<Map<String, Object>> runLoadTest(
            @RequestParam(required = false) Integer qps,
            @RequestParam(required = false) Integer duration,
            @RequestParam(required = false) String baseUrl,
            @RequestParam String token) {
        if (!properties.getPerformance().getToken().equals(token)) {
            return ApiResponse.error(403, "性能压测 token 无效");
        }
        return ApiResponse.success(loadTestService.runLoadTest(qps, duration, baseUrl));
    }

    @GetMapping("/cache-comparison")
    public ApiResponse<Map<String, Object>> cacheComparison() {
        RankingCacheManager.CacheStats stats = cacheManager.getStats();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cacheStats", stats);
        result.put("note", "连续请求相同榜单接口后，hitRate 会反映当前进程内本地缓存命中情况。");
        return ApiResponse.success(result);
    }
}
