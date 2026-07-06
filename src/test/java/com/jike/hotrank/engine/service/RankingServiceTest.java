package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.dto.RankingResponseDTO;
import com.jike.hotrank.engine.entity.Topic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @Mock
    private TopicService topicService;

    @Mock
    private CircleService circleService;

    @Mock
    private UserCirclePreferenceService userCirclePreferenceService;

    @Mock
    private InteractionEventService interactionEventService;

    @InjectMocks
    private RankingService rankingService;

    @Test
    void shouldBuildSurgingRankingFromHourlyInteractionGrowth() {
        when(interactionEventService.aggregateWeightedScoreByTopic(any(), any()))
            .thenReturn(List.of(
                Map.of("topic_id", 1L, "weighted_score", 10L, "total_count", 5L),
                Map.of("topic_id", 2L, "weighted_score", 8L, "total_count", 4L),
                Map.of("topic_id", 3L, "weighted_score", 3L, "total_count", 3L)
            ))
            .thenReturn(List.of(
                Map.of("topic_id", 1L, "weighted_score", 5L, "total_count", 2L),
                Map.of("topic_id", 3L, "weighted_score", 1L, "total_count", 1L)
            ));
        when(topicService.getById(1L)).thenReturn(topic(1L, "一号"));
        when(topicService.getById(2L)).thenReturn(topic(2L, "二号"));
        when(topicService.getById(3L)).thenReturn(topic(3L, "三号"));

        RankingResponseDTO response = rankingService.getSurgingRanking(3);

        assertEquals("surging", response.getRankingType());
        assertEquals(List.of(2L, 3L, 1L), response.getItems().stream()
            .map(item -> item.getTopicId())
            .toList());
        assertEquals(new BigDecimal("8.0000"), response.getItems().getFirst().getScore());
        assertEquals(new BigDecimal("3.0000"), response.getItems().get(1).getScore());
        assertEquals(new BigDecimal("2.0000"), response.getItems().get(2).getScore());
        verify(topicService, never()).getGlobalHotRank(eq(3));
    }

    private Topic topic(Long id, String title) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setCircleId(1L);
        topic.setTitle(title);
        topic.setAuthorId(100L + id);
        topic.setPublishTime(LocalDateTime.now().minusHours(2));
        topic.setCurrentScore(BigDecimal.ZERO);
        topic.setInteractionCount(0);
        topic.setStatus(1);
        return topic;
    }
}
