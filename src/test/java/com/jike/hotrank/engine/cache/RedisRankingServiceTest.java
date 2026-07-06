package com.jike.hotrank.engine.cache;

import com.jike.hotrank.engine.dto.RankingItemDTO;
import com.jike.hotrank.engine.dto.RankingResponseDTO;
import com.jike.hotrank.engine.entity.Circle;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.CircleService;
import com.jike.hotrank.engine.service.TopicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRankingServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private TopicService topicService;

    @Mock
    private CircleService circleService;

    private RedisRankingService redisRankingService;

    @BeforeEach
    void setUp() {
        redisRankingService = new RedisRankingService(redisTemplate, topicService, circleService);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void syncAllTopicsShouldReplaceRedisDataAndSkipInactiveTopics() {
        Topic active = topic(1L, 2L, 1, "Active", "8.5");
        Topic blocked = topic(2L, 2L, 0, "Blocked", "99.0");
        Set<String> circleKeys = Set.of("ranking:circle:2", "ranking:circle:9");
        when(redisTemplate.keys("ranking:circle:*")).thenReturn(circleKeys);

        redisRankingService.syncAllTopics(List.of(active, blocked));

        verify(redisTemplate).delete("ranking:global");
        verify(redisTemplate).delete(circleKeys);
        verify(zSetOperations).add("ranking:global", "1", 8.5);
        verify(zSetOperations).add("ranking:circle:2", "1", 8.5);
        verify(zSetOperations, never()).add(eq("ranking:global"), eq("2"), anyDouble());
    }

    @Test
    void getGlobalRankingShouldSkipInactiveMissingAndInvalidRedisMembers() {
        Topic active = topic(1L, 2L, 1, "Active", "8.12345");
        Topic blocked = topic(2L, 2L, 0, "Blocked", "99.0");
        Circle circle = new Circle();
        circle.setId(2L);
        circle.setName("Tech");
        Set<ZSetOperations.TypedTuple<String>> redisTuples = tuples(
            tuple("1", 8.12345),
            tuple("2", 99.0),
            tuple("3", 7.0),
            tuple("bad-member", 6.0)
        );

        when(zSetOperations.reverseRangeWithScores("ranking:global", 0, 9))
            .thenReturn(redisTuples);
        when(topicService.getById(1L)).thenReturn(active);
        when(topicService.getById(2L)).thenReturn(blocked);
        when(topicService.getById(3L)).thenReturn(null);
        when(circleService.getById(2L)).thenReturn(circle);

        RankingResponseDTO response = redisRankingService.getGlobalRanking(10);

        assertEquals(1, response.getItems().size());
        RankingItemDTO item = response.getItems().getFirst();
        assertEquals(1, item.getRank());
        assertEquals(1L, item.getTopicId());
        assertEquals("Active", item.getTitle());
        assertEquals("Tech", item.getCircleName());
        assertEquals(new BigDecimal("8.1235"), item.getScore());
    }

    @Test
    void removeTopicShouldDeleteGlobalAndCircleMembers() {
        redisRankingService.removeTopic(7L, 3L);

        verify(zSetOperations).remove("ranking:global", "7");
        verify(zSetOperations).remove("ranking:circle:3", "7");
    }

    @SafeVarargs
    private Set<ZSetOperations.TypedTuple<String>> tuples(ZSetOperations.TypedTuple<String>... values) {
        return new LinkedHashSet<>(List.of(values));
    }

    private ZSetOperations.TypedTuple<String> tuple(String value, double score) {
        ZSetOperations.TypedTuple<String> tuple = org.mockito.Mockito.mock(ZSetOperations.TypedTuple.class);
        when(tuple.getValue()).thenReturn(value);
        when(tuple.getScore()).thenReturn(score);
        return tuple;
    }

    private Topic topic(Long id, Long circleId, Integer status, String title, String score) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setCircleId(circleId);
        topic.setStatus(status);
        topic.setTitle(title);
        topic.setCurrentScore(new BigDecimal(score));
        topic.setInteractionCount(12);
        topic.setAuthorId(100L + id);
        return topic;
    }
}
