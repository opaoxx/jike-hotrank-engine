package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.entity.Circle;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.entity.UserBehavior;
import com.jike.hotrank.engine.mapper.UserBehaviorMapper;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private TopicService topicService;

    @Mock
    private CircleService circleService;

    @Mock
    private InteractionEventService interactionEventService;

    @Mock
    private UserBehaviorMapper userBehaviorMapper;

    @Mock
    private RankingCacheManager cacheManager;

    @InjectMocks
    private AnalysisService analysisService;

    @Test
    void shouldCalculateHeatDistribution() {
        when(topicService.listByStatus(1)).thenReturn(List.of(
            topic(1L, 1L, "low", "5", 1),
            topic(2L, 1L, "mid", "30", 2),
            topic(3L, 2L, "high", "120", 3)
        ));

        Map<String, Object> result = analysisService.heatDistribution();

        assertEquals(3, result.get("topicCount"));
        assertEquals(new BigDecimal("120"), result.get("maxScore"));
        assertEquals(new BigDecimal("30"), result.get("medianScore"));
        assertFalse(((List<?>) result.get("ranges")).isEmpty());
    }

    @Test
    void shouldCalculateInteractionStats() {
        when(interactionEventService.aggregateByType(any(), any())).thenReturn(List.of(
            Map.of("interaction_type", 1, "total_count", 8L),
            Map.of("interaction_type", 5, "total_count", 2L)
        ));

        Map<String, Object> result = analysisService.interactionStats(24);

        assertEquals(24, result.get("hours"));
        assertEquals(10L, result.get("total"));
        List<?> byType = (List<?>) result.get("byType");
        assertEquals(2, byType.size());
    }

    @Test
    void shouldCalculateCircleActivity() {
        Circle circle = new Circle();
        circle.setId(1L);
        circle.setName("科技圈");
        when(circleService.listAllEnabled()).thenReturn(List.of(circle));
        when(topicService.listByStatus(1)).thenReturn(List.of(
            topic(1L, 1L, "a", "10", 4),
            topic(2L, 1L, "b", "20", 6)
        ));

        Map<String, Object> result = analysisService.circleActivity();

        List<?> items = (List<?>) result.get("items");
        Map<?, ?> first = (Map<?, ?>) items.getFirst();
        assertEquals("科技圈", first.get("circleName"));
        assertEquals(10L, first.get("interactionCount"));
        assertEquals(new BigDecimal("15.0000"), first.get("avgScore"));
    }

    @Test
    void shouldCalculateAntiCheatStats() {
        UserBehavior behavior = new UserBehavior();
        behavior.setTopicId(1L);
        behavior.setUserId(99L);
        behavior.setInvalidReason("rate_limit");
        behavior.setCreatedAt(LocalDateTime.now());
        when(userBehaviorMapper.selectInvalidBehaviors(any(), any())).thenReturn(List.of(behavior));

        Map<String, Object> result = analysisService.antiCheatStats(7);

        assertEquals(7, result.get("days"));
        assertEquals(1, result.get("totalBlockedCount"));
        assertEquals(Map.of("rate_limit", 1L), result.get("byReason"));
    }

    private Topic topic(Long id, Long circleId, String title, String score, Integer interactionCount) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setCircleId(circleId);
        topic.setTitle(title);
        topic.setCurrentScore(new BigDecimal(score));
        topic.setInteractionCount(interactionCount);
        return topic;
    }
}
