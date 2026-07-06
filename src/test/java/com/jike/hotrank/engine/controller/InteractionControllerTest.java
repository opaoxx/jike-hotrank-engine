package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.entity.InteractionEvent;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.AntiSpamService;
import com.jike.hotrank.engine.service.InteractionEventService;
import com.jike.hotrank.engine.service.TopicService;
import com.jike.hotrank.engine.service.UserBehaviorService;
import com.jike.hotrank.engine.service.UserCirclePreferenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionControllerTest {

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
    private InteractionController interactionController;

    @Test
    void shouldEvictPersonalizedRankingCacheAfterPreferenceUpdate() {
        InteractionEvent event = new InteractionEvent();
        event.setTopicId(1L);
        event.setUserId(99L);
        event.setInteractionType(1);
        event.setDeviceFingerprint("device-a");

        Topic topic = new Topic();
        topic.setId(1L);
        topic.setCircleId(10L);
        topic.setStatus(1);
        topic.setPublishTime(LocalDateTime.now());

        when(topicService.getById(1L)).thenReturn(topic);
        when(antiSpamService.checkInteraction(99L, 1L, "device-a"))
            .thenReturn(AntiSpamService.CheckResult.allow(BigDecimal.ONE, "allow"));
        when(interactionEventService.record(event)).thenReturn(event);

        ApiResponse<InteractionEvent> response = interactionController.recordInteraction(event);

        assertEquals(0, response.getCode());
        verify(userCirclePreferenceService).updatePreference(99L, 10L);
        verify(cacheManager).evictByPrefix(RankingCacheManager.personalizedRankPrefix(99L));
    }
}
