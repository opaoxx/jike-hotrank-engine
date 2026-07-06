package com.jike.hotrank.engine.task;

import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.InteractionEventService;
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

    @InjectMocks
    private HeatAggregationTask heatAggregationTask;

    @Test
    @SuppressWarnings("unchecked")
    void shouldRecalculateHeatFromAllInteractionsWithoutAddingExistingScore() {
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setStatus(1);
        topic.setPublishTime(LocalDateTime.now());
        topic.setCurrentScore(BigDecimal.valueOf(999));
        topic.setInteractionCount(100);

        when(topicService.getGlobalHotRank(anyInt())).thenReturn(List.of());
        when(interactionEventService.aggregateAllByTopic()).thenReturn(List.of(
            Map.of("topic_id", 1L, "interaction_type", 1, "total_count", 4L),
            Map.of("topic_id", 1L, "interaction_type", 5, "total_count", 2L)
        ));
        when(topicService.getById(1L)).thenReturn(topic);

        heatAggregationTask.aggregateHeat();

        ArgumentCaptor<List<Topic>> captor = ArgumentCaptor.forClass(List.class);
        verify(topicService).batchUpdateScore(captor.capture());
        Topic updated = captor.getValue().getFirst();

        assertEquals(1L, updated.getId());
        assertEquals(6, updated.getInteractionCount());
        assertTrue(updated.getCurrentScore().compareTo(BigDecimal.TEN) < 0,
            "全量重算应只使用本次聚合出的加权互动分，不应叠加旧currentScore");
        verify(interactionEventService, never()).aggregateByTopic(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
