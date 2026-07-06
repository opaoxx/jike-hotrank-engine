package com.jike.hotrank.engine.cache;

import com.jike.hotrank.engine.dto.RankingItemDTO;
import com.jike.hotrank.engine.dto.RankingResponseDTO;
import com.jike.hotrank.engine.entity.Circle;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.CircleService;
import com.jike.hotrank.engine.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 基于 Redis ZSet 的排名服务。
 *
 * @author JikeHotRank Team
 * <p>
 * 与 MySQL 窗口函数方案形成对比：
 * - Redis ZSet: O(logN) 插入 + O(logN+M) 范围查询，适合高并发实时场景
 * - MySQL RANK(): O(N log N) 排序，适合中小规模、需要复杂 SQL 的场景
 * <p>
 * Key 设计：
 * - ranking:global        → 全站热榜 ZSet
 * - ranking:circle:{id}   → 圈子热榜 ZSet
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRankingService {

    private static final String KEY_GLOBAL = "ranking:global";
    private static final String KEY_CIRCLE_PREFIX = "ranking:circle:";

    private final StringRedisTemplate redisTemplate;
    private final TopicService topicService;
    private final CircleService circleService;

    /**
     * 直接设置话题分数（定时聚合任务调用，覆盖写）。
     */
    public void setScore(Long topicId, Long circleId, double score) {
        String member = String.valueOf(topicId);
        redisTemplate.opsForZSet().add(KEY_GLOBAL, member, score);
        if (circleId != null) {
            redisTemplate.opsForZSet().add(KEY_CIRCLE_PREFIX + circleId, member, score);
        }
    }

    /**
     * 全站热榜 TOP N — O(logN+M) 复杂度。
     */
    public RankingResponseDTO getGlobalRanking(int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
            redisTemplate.opsForZSet().reverseRangeWithScores(KEY_GLOBAL, 0, limit - 1);

        List<RankingItemDTO> items = convertToRankingItems(tuples);
        return RankingResponseDTO.ofGlobal(items);
    }

    /**
     * 圈子热榜 TOP N。
     */
    public RankingResponseDTO getCircleRanking(Long circleId, int limit) {
        String key = KEY_CIRCLE_PREFIX + circleId;
        Set<ZSetOperations.TypedTuple<String>> tuples =
            redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        Circle circle = circleService.getById(circleId);
        String circleName = circle != null ? circle.getName() : "未知圈子";
        List<RankingItemDTO> items = convertToRankingItems(tuples, circleName);
        return RankingResponseDTO.ofCircle(circleId, circleName, items);
    }

    /**
     * 批量同步全量数据到 Redis（定时任务调用，每5分钟一次）。
     */
    public void syncAllTopics(List<Topic> topics) {
        for (Topic topic : topics) {
            double score = topic.getCurrentScore() != null
                ? topic.getCurrentScore().doubleValue() : 0.0;
            setScore(topic.getId(), topic.getCircleId(), score);
        }
        log.info("Redis ranking sync completed: {} topics", topics.size());
    }

    /**
     * 清空所有排名数据。
     */
    public void flushAll() {
        // 清空全站榜
        redisTemplate.delete(KEY_GLOBAL);
        // 清空圈子榜（KEYS 在数据量大时应改用 SCAN，演示场景可接受）
        Set<String> circleKeys = redisTemplate.keys(KEY_CIRCLE_PREFIX + "*");
        if (circleKeys != null && !circleKeys.isEmpty()) {
            redisTemplate.delete(circleKeys);
        }
        log.info("Redis ranking data flushed");
    }

    private List<RankingItemDTO> convertToRankingItems(Set<ZSetOperations.TypedTuple<String>> tuples) {
        return convertToRankingItems(tuples, null);
    }

    private List<RankingItemDTO> convertToRankingItems(Set<ZSetOperations.TypedTuple<String>> tuples, String circleName) {
        List<RankingItemDTO> items = new ArrayList<>();
        if (tuples == null) {
            return items;
        }

        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            String member = tuple.getValue();
            Double score = tuple.getScore();
            if (member == null) {
                continue;
            }

            Long topicId = Long.parseLong(member);
            Topic topic = topicService.getById(topicId);
            if (topic == null) {
                continue;
            }

            RankingItemDTO item = new RankingItemDTO();
            item.setRank(rank++);
            item.setTopicId(topicId);
            item.setTitle(topic.getTitle());
            item.setCircleId(topic.getCircleId());
            item.setCircleName(circleName != null ? circleName : getCircleName(topic.getCircleId()));
            item.setScore(score != null ? BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            item.setInteractionCount(topic.getInteractionCount());
            item.setAuthorId(topic.getAuthorId());
            item.setPublishTime(topic.getPublishTime() != null ? topic.getPublishTime().toString() : null);
            items.add(item);
        }
        return items;
    }

    private String getCircleName(Long circleId) {
        if (circleId == null) {
            return "未知圈子";
        }
        Circle circle = circleService.getById(circleId);
        return circle != null ? circle.getName() : "未知圈子";
    }
}
