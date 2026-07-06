package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/topic")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final RankingCacheManager cacheManager;

    @GetMapping("/{id}")
    public ApiResponse<Topic> getTopic(@PathVariable Long id) {
        if (isInvalidId(id)) {
            return ApiResponse.error(400, "话题ID无效");
        }

        Topic topic = topicService.getById(id);
        if (topic == null) {
            return ApiResponse.error(404, "话题不存在");
        }
        return ApiResponse.success(topic);
    }

    @PostMapping("/{id}/block")
    public ApiResponse<Void> blockTopic(@PathVariable Long id) {
        log.info("Block topic: topicId={}", id);
        if (isInvalidId(id)) {
            return ApiResponse.error(400, "话题ID无效");
        }

        Topic topic = topicService.getById(id);
        if (topic == null) {
            return ApiResponse.error(404, "话题不存在");
        }
        if (topic.getStatus() == 0) {
            return ApiResponse.error(400, "话题已被屏蔽");
        }

        int rows = topicService.blockTopic(id);
        if (rows <= 0) {
            return ApiResponse.error(500, "屏蔽失败");
        }

        cacheManager.evictAll();
        log.info("Topic blocked and ranking cache evicted: topicId={}", id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/unblock")
    public ApiResponse<Void> unblockTopic(@PathVariable Long id) {
        log.info("Unblock topic: topicId={}", id);
        if (isInvalidId(id)) {
            return ApiResponse.error(400, "话题ID无效");
        }

        Topic topic = topicService.getById(id);
        if (topic == null) {
            return ApiResponse.error(404, "话题不存在");
        }
        if (topic.getStatus() != 0) {
            return ApiResponse.error(400, "话题未被屏蔽");
        }

        int rows = topicService.unblockTopic(id);
        if (rows <= 0) {
            return ApiResponse.error(500, "恢复失败");
        }

        cacheManager.evictAll();
        log.info("Topic unblocked and ranking cache evicted: topicId={}", id);
        return ApiResponse.success();
    }

    private boolean isInvalidId(Long id) {
        return id == null || id <= 0;
    }
}
