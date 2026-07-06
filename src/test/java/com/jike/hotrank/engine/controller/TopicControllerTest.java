package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.cache.RedisRankingService;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.TopicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicControllerTest {

    @Mock
    private TopicService topicService;

    @Mock
    private RankingCacheManager cacheManager;

    @Mock
    private RedisRankingService redisRankingService;

    @InjectMocks
    private TopicController topicController;

    @Test
    void shouldRejectInvalidTopicIdBeforeQueryingDatabase() {
        ApiResponse<Topic> response = topicController.getTopic(0L);

        assertEquals(400, response.getCode());
        assertEquals("话题ID无效", response.getMessage());
        verify(topicService, never()).getById(0L);
    }

    @Test
    void shouldEvictRankingCacheAfterBlockingTopic() {
        Topic topic = new Topic();
        topic.setId(1L);
        topic.setStatus(1);
        when(topicService.getById(1L)).thenReturn(topic);
        when(topicService.blockTopic(1L)).thenReturn(1);

        ApiResponse<Void> response = topicController.blockTopic(1L);

        assertEquals(0, response.getCode());
        verify(cacheManager).evictAll();
        verify(redisRankingService).removeTopic(1L, null);
    }
}
