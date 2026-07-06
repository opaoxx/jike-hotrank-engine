package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.config.HotRankProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AntiSpamServiceTest {

    @Mock
    private InteractionEventService interactionEventService;

    @Mock
    private TopicService topicService;

    private HotRankProperties properties;
    private AntiSpamService antiSpamService;

    @BeforeEach
    void setUp() {
        properties = new HotRankProperties();
        antiSpamService = new AntiSpamService(interactionEventService, topicService, properties);
    }

    @Test
    void shouldRejectWhenFrequencyLimitReached() {
        properties.getAntiSpam().getFrequency().setMaxInteractions(2);
        when(interactionEventService.countByUserAndTopic(10L, 20L)).thenReturn(2);

        AntiSpamService.CheckResult result = antiSpamService.checkInteraction(10L, 20L, "device-a");

        assertFalse(result.allowed());
        assertEquals(BigDecimal.ZERO, result.weightMultiplier());
        assertEquals("rate_limit", result.reason());
        verify(interactionEventService, never()).countDistinctUserByDevice(any());
    }

    @Test
    void shouldAllowWithFirstLevelDevicePenalty() {
        when(interactionEventService.countByUserAndTopic(10L, 20L)).thenReturn(0);
        when(interactionEventService.countDistinctUserByDevice("device-a")).thenReturn(2);
        when(interactionEventService.hasUserUsedDevice(10L, "device-a")).thenReturn(false);

        AntiSpamService.CheckResult result = antiSpamService.checkInteraction(10L, 20L, "device-a");

        assertTrue(result.allowed());
        assertEquals(new BigDecimal("0.5"), result.weightMultiplier());
        assertEquals("device_fingerprint_penalty", result.reason());
    }

    @Test
    void shouldAllowWithSecondLevelDevicePenalty() {
        when(interactionEventService.countByUserAndTopic(10L, 20L)).thenReturn(0);
        when(interactionEventService.countDistinctUserByDevice("device-a")).thenReturn(4);
        when(interactionEventService.hasUserUsedDevice(10L, "device-a")).thenReturn(false);

        AntiSpamService.CheckResult result = antiSpamService.checkInteraction(10L, 20L, "device-a");

        assertTrue(result.allowed());
        assertEquals(new BigDecimal("0.3"), result.weightMultiplier());
    }

    @Test
    void shouldMarkTopicForReviewWhenSurgeIsAbnormal() {
        properties.getAntiSpam().getSurge().setMinimumCurrentCount(10);
        properties.getAntiSpam().getSurge().setMultiplierThreshold(10);
        when(interactionEventService.countByTopicAndTimeRange(eq(20L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(20, 10);

        boolean normal = antiSpamService.checkAnomalySpike(20L);

        assertFalse(normal);
        verify(topicService).markForReview(20L);
    }
}
