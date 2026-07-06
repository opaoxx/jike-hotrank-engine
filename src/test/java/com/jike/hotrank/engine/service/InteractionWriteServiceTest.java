package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.entity.InteractionEvent;
import com.jike.hotrank.engine.entity.Topic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionWriteServiceTest {

    @Mock
    private InteractionEventService interactionEventService;

    @Mock
    private AntiSpamService antiSpamService;

    @Mock
    private UserBehaviorService userBehaviorService;

    @Mock
    private TopicService topicService;

    @Mock
    private UserCirclePreferenceService userCirclePreferenceService;

    @Mock
    private RankingCacheManager cacheManager;

    @InjectMocks
    private InteractionWriteService interactionWriteService;

    @Test
    void shouldRecordInteractionAndEvictPersonalizedCache() {
        InteractionEvent event = interactionEvent();
        Topic topic = topic();

        when(topicService.getById(1L)).thenReturn(topic);
        when(antiSpamService.checkInteraction(99L, 1L, "device-a"))
            .thenReturn(AntiSpamService.CheckResult.allow(BigDecimal.ONE, "allow"));
        when(interactionEventService.record(event)).thenReturn(event);

        InteractionWriteService.RecordResult result = interactionWriteService.recordInteraction(event);

        assertTrue(result.accepted());
        assertEquals(event, result.event());
        verify(userBehaviorService).recordValid(99L, 1L, 1, "device-a", "127.0.0.1");
        verify(interactionEventService).record(event);
        verify(userCirclePreferenceService).updatePreference(99L, 10L);
        verify(cacheManager).evictByPrefix(RankingCacheManager.personalizedRankPrefix(99L));
        verify(antiSpamService).checkAnomalySpike(1L);
    }

    @Test
    void shouldPersistInvalidBehaviorAndSkipInteractionWriteWhenRejected() {
        InteractionEvent event = interactionEvent();
        Topic topic = topic();

        when(topicService.getById(1L)).thenReturn(topic);
        when(antiSpamService.checkInteraction(99L, 1L, "device-a"))
            .thenReturn(AntiSpamService.CheckResult.deny("rate_limit"));

        InteractionWriteService.RecordResult result = interactionWriteService.recordInteraction(event);

        assertEquals(429, result.code());
        verify(userBehaviorService).recordInvalid(99L, 1L, 1, "device-a", "127.0.0.1", "rate_limit");
        verify(interactionEventService, never()).record(event);
        verify(userCirclePreferenceService, never()).updatePreference(99L, 10L);
    }

    private InteractionEvent interactionEvent() {
        InteractionEvent event = new InteractionEvent();
        event.setTopicId(1L);
        event.setUserId(99L);
        event.setInteractionType(1);
        event.setDeviceFingerprint("device-a");
        event.setIpAddress("127.0.0.1");
        return event;
    }

    private Topic topic() {
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setCircleId(10L);
        topic.setStatus(1);
        topic.setPublishTime(LocalDateTime.now());
        return topic;
    }
}
