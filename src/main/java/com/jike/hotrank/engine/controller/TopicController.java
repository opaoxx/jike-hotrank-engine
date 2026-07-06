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

/**
 * 话题控制器
 * 提供话题管理接口，包括话题屏蔽功能
 *
 * @author JikeHotRank Team
 */
@Slf4j
@RestController
@RequestMapping("/api/topic")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;
    private final RankingCacheManager cacheManager;

    /**
     * 根据ID查询话题
     *
     * @param id 话题ID
     * @return 话题信息
     */
    @GetMapping("/{id}")
    public ApiResponse<Topic> getTopic(@PathVariable Long id) {
        Topic topic = topicService.getById(id);
        if (topic == null) {
            return ApiResponse.error(404, "话题不存在");
        }
        return ApiResponse.success(topic);
    }

    /**
     * 屏蔽话题
     * <p>
     * 屏蔽后立即清除所有榜单缓存中的该话题
     * 屏蔽话题不参与热度聚合
     *
     * @param id 话题ID
     * @return 操作结果
     */
    @PostMapping("/{id}/block")
    public ApiResponse<Void> blockTopic(@PathVariable Long id) {
        log.info("屏蔽话题：topicId={}", id);

        Topic topic = topicService.getById(id);
        if (topic == null) {
            return ApiResponse.error(404, "话题不存在");
        }

        if (topic.getStatus() == 0) {
            return ApiResponse.error(400, "话题已被屏蔽");
        }

        // 屏蔽话题
        int rows = topicService.blockTopic(id);
        if (rows > 0) {
            // 清除所有榜单缓存（话题屏蔽后应立即从榜单消失）
            cacheManager.evictAll();
            log.info("话题屏蔽成功，已清除所有榜单缓存：topicId={}", id);
            return ApiResponse.success();
        } else {
            return ApiResponse.error(500, "屏蔽失败");
        }
    }

    /**
     * 恢复话题
     *
     * @param id 话题ID
     * @return 操作结果
     */
    @PostMapping("/{id}/unblock")
    public ApiResponse<Void> unblockTopic(@PathVariable Long id) {
        log.info("恢复话题：topicId={}", id);

        Topic topic = topicService.getById(id);
        if (topic == null) {
            return ApiResponse.error(404, "话题不存在");
        }

        if (topic.getStatus() != 0) {
            return ApiResponse.error(400, "话题未被屏蔽");
        }

        // 恢复话题
        int rows = topicService.unblockTopic(id);
        if (rows > 0) {
            // 清除所有榜单缓存
            cacheManager.evictAll();
            log.info("话题恢复成功，已清除所有榜单缓存：topicId={}", id);
            return ApiResponse.success();
        } else {
            return ApiResponse.error(500, "恢复失败");
        }
    }
}
