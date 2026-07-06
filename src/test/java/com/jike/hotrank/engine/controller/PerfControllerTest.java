package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.config.HotRankProperties;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.service.LoadTestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfControllerTest {

    @Mock
    private LoadTestService loadTestService;

    @Mock
    private RankingCacheManager cacheManager;

    private final HotRankProperties properties = new HotRankProperties();

    @InjectMocks
    private PerfController perfController;

    @Test
    void shouldRejectInvalidLoadTestToken() {
        PerfController controller = new PerfController(loadTestService, cacheManager, properties);

        ApiResponse<Map<String, Object>> response = controller.runLoadTest(10, 1, null, "wrong");

        assertEquals(403, response.getCode());
        verify(loadTestService, never()).runLoadTest(10, 1, null);
    }

    @Test
    void shouldDelegateLoadTestWhenTokenIsValid() {
        PerfController controller = new PerfController(loadTestService, cacheManager, properties);
        when(loadTestService.runLoadTest(10, 1, null)).thenReturn(Map.of("totalRequests", 10));

        ApiResponse<Map<String, Object>> response = controller.runLoadTest(10, 1, null, "perf_test_token");

        assertEquals(0, response.getCode());
        assertEquals(10, response.getData().get("totalRequests"));
    }

    @Test
    void shouldExposeCacheStats() {
        PerfController controller = new PerfController(loadTestService, cacheManager, properties);
        when(cacheManager.getStats()).thenReturn(new RankingCacheManager.CacheStats(1, 2, 3, 0, 4, 0, 0.4));

        ApiResponse<Map<String, Object>> response = controller.cacheComparison();

        assertEquals(0, response.getCode());
        assertEquals(1L, ((RankingCacheManager.CacheStats) response.getData().get("cacheStats")).size());
    }
}
