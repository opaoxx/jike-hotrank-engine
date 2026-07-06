package com.jike.hotrank.engine.task;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.cache.RedisRankingService;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.InteractionEventService;
import com.jike.hotrank.engine.service.RankingNotificationService;
import com.jike.hotrank.engine.service.TaskLockService;
import com.jike.hotrank.engine.service.TopicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeatAggregationTaskTest {

    @Mock
    private InteractionEventService interactionEventService;

    @Mock
    private TopicService topicService;

    @Mock
    private RankingCacheManager cacheManager;

    @Mock
    private RankingNotificationService rankingNotificationService;

    @Mock
    private TaskLockService taskLockService;

    @Mock
    private RedisRankingService redisRankingService;

    @InjectMocks
    private HeatAggregationTask heatAggregationTask;

    @Test
    void shouldRunScheduledAggregationThroughTaskLock() {
        heatAggregationTask.aggregateHeatWithLock();

        verify(taskLockService).runWithLock(org.mockito.ArgumentMatchers.eq("jike-hotrank:heat-aggregation"),
            org.mockito.ArgumentMatchers.any(Runnable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRecalculateHeatFromWeightedInteractionsWithoutAddingExistingScore() {
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setStatus(1);
        topic.setPublishTime(LocalDateTime.now());
        topic.setCurrentScore(BigDecimal.valueOf(999));
        topic.setInteractionCount(100);

        when(topicService.getGlobalHotRank(anyInt())).thenReturn(List.of());
        when(interactionEventService.aggregateWeightedScoreAllByTopic()).thenReturn(List.of(
            Map.of("topic_id", 1L, "weighted_score", new BigDecimal("14.0"), "total_count", 6L)
        ));
        when(topicService.getById(1L)).thenReturn(topic);
        when(topicService.listByStatus(1)).thenReturn(List.of(topic));

        heatAggregationTask.aggregateHeat();

        ArgumentCaptor<List<Topic>> captor = ArgumentCaptor.forClass(List.class);
        verify(topicService).batchUpdateScore(captor.capture());
        Topic updated = captor.getValue().getFirst();

        assertEquals(1L, updated.getId());
        assertEquals(6, updated.getInteractionCount());
        assertTrue(updated.getCurrentScore().compareTo(BigDecimal.TEN) < 0,
            "Heat should be recalculated from weighted interaction score only, not existing currentScore.");
        verify(cacheManager).evictByPrefix(RankingCacheManager.rankingPrefix());
        verify(redisRankingService).syncAllTopics(List.of(topic));
        verify(rankingNotificationService).publishRankingUpdated(1);
        verify(interactionEventService, never()).aggregateAllByTopic();
    }
}
