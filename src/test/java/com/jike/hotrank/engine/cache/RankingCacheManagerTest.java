package com.jike.hotrank.engine.cache;

import com.jike.hotrank.engine.dto.RankingResponseDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankingCacheManagerTest {

    @Test
    void shouldDistinguishNullHitFromMiss() {
        RankingCacheManager cacheManager = new RankingCacheManager();
        try {
            assertFalse(cacheManager.getResult("ranking:missing").hit());

            cacheManager.putNull("ranking:empty");

            RankingCacheManager.CacheResult result = cacheManager.getResult("ranking:empty");
            assertTrue(result.hit());
            assertNull(result.data());
        } finally {
            cacheManager.shutdown();
        }
    }

    @Test
    void shouldCacheEmptyRankingResponseAsNormalData() {
        RankingCacheManager cacheManager = new RankingCacheManager();
        try {
            RankingResponseDTO response = RankingResponseDTO.ofGlobal(List.of());

            cacheManager.put("ranking:global:50", response);

            RankingCacheManager.CacheResult result = cacheManager.getResult("ranking:global:50");
            assertTrue(result.hit());
            assertSame(response, result.data());
        } finally {
            cacheManager.shutdown();
        }
    }
}
