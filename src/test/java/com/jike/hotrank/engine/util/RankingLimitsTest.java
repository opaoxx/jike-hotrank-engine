package com.jike.hotrank.engine.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingLimitsTest {

    @Test
    void shouldUseDefaultsForNullOrNonPositiveLimits() {
        assertEquals(50, RankingLimits.global(null));
        assertEquals(50, RankingLimits.global(0));
        assertEquals(20, RankingLimits.circle(-1));
        assertEquals(10, RankingLimits.newcomer(null));
        assertEquals(10, RankingLimits.surging(null));
    }

    @Test
    void shouldCapOversizedLimits() {
        assertEquals(100, RankingLimits.global(9999));
        assertEquals(100, RankingLimits.circle(9999));
        assertEquals(50, RankingLimits.newcomer(9999));
        assertEquals(50, RankingLimits.surging(9999));
        assertEquals(200, RankingLimits.personalizedCandidate(9999));
    }
}
