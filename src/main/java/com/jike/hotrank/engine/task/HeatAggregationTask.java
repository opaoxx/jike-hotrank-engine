package com.jike.hotrank.engine.task;

import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.InteractionEventService;
import com.jike.hotrank.engine.service.TopicService;
import com.jike.hotrank.engine.util.HeatScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 热度聚合定时任务
 * <p>
 * 每5分钟执行一次，从互动事件表聚合数据，计算时间衰减热度分，更新话题表
 * <p>
 * 包含上榜事件触发逻辑：话题首次进入TOP10时触发通知
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HeatAggregationTask {

    private final InteractionEventService interactionEventService;
    private final TopicService topicService;

    /** 上榜事件触发阈值（TOP N） */
    private static final int TOP_N_THRESHOLD = 10;

    /**
     * 热度聚合任务
     * <p>
     * 执行频率：每5分钟（300000毫秒）
     * <p>
     * 执行逻辑：
     * 1. 查询最近5分钟内的互动事件
     * 2. 按话题聚合，计算各类型互动次数
     * 3. 使用时间衰减算法计算热度分
     * 4. 更新话题表的热度分和互动次数
     * 5. 检测上榜事件并触发通知
     */
    @Scheduled(fixedRate = 300000)  // 5分钟 = 300000毫秒
    public void aggregateHeat() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(5);

        log.info("开始执行热度聚合任务：startTime={}, endTime={}", startTime, endTime);

        try {
            // 1. 查询聚合前的TOP N话题ID（用于上榜事件检测）
            Set<Long> previousTopTopicIds = getTopTopicIds(TOP_N_THRESHOLD);

            // 2. 查询最近5分钟内的互动事件聚合数据
            List<Map<String, Object>> aggregationData = interactionEventService.aggregateByTopic(startTime, endTime);

            if (aggregationData == null || aggregationData.isEmpty()) {
                log.info("热度聚合任务完成：无新互动事件");
                return;
            }

            // 3. 按话题ID分组聚合
            Map<Long, Map<Integer, Long>> topicInteractionMap = new HashMap<>();
            for (Map<String, Object> row : aggregationData) {
                Long topicId = ((Number) row.get("topic_id")).longValue();
                Integer interactionType = ((Number) row.get("interaction_type")).intValue();
                Long count = ((Number) row.get("total_count")).longValue();

                topicInteractionMap
                    .computeIfAbsent(topicId, k -> new HashMap<>())
                    .put(interactionType, count);
            }

            // 4. 计算每个话题的新热度分
            List<Topic> topicsToUpdate = new ArrayList<>();
            for (Map.Entry<Long, Map<Integer, Long>> entry : topicInteractionMap.entrySet()) {
                Long topicId = entry.getKey();
                Map<Integer, Long> interactionCounts = entry.getValue();

                // 查询话题信息
                Topic topic = topicService.getById(topicId);
                if (topic == null || topic.getStatus() != 1) {
                    log.warn("话题不存在或已屏蔽，跳过：topicId={}", topicId);
                    continue;
                }

                // 计算互动分
                long totalInteractionScore = 0;
                int totalInteractionCount = 0;
                for (Map.Entry<Integer, Long> interactionEntry : interactionCounts.entrySet()) {
                    int type = interactionEntry.getKey();
                    long count = interactionEntry.getValue();
                    int weight = HeatScoreCalculator.getWeight(type);
                    totalInteractionScore += count * weight;
                    totalInteractionCount += count;
                }

                // 累加到现有互动次数
                int newInteractionCount = topic.getInteractionCount() + totalInteractionCount;

                // 使用时间衰减算法计算热度分
                BigDecimal newScore = HeatScoreCalculator.calculateScore(
                    totalInteractionScore + topic.getCurrentScore().longValue(),
                    topic.getPublishTime()
                );

                // 准备更新数据
                Topic updateTopic = new Topic();
                updateTopic.setId(topicId);
                updateTopic.setCurrentScore(newScore);
                updateTopic.setInteractionCount(newInteractionCount);
                topicsToUpdate.add(updateTopic);
            }

            // 5. 批量更新话题热度分
            if (!topicsToUpdate.isEmpty()) {
                topicService.batchUpdateScore(topicsToUpdate);
                log.info("热度聚合任务完成：更新话题数量={}", topicsToUpdate.size());

                // 6. 检测上榜事件
                checkTopNEvent(previousTopTopicIds, TOP_N_THRESHOLD);
            }

        } catch (Exception e) {
            log.error("热度聚合任务执行失败", e);
        }
    }

    /**
     * 获取当前TOP N话题ID集合
     *
     * @param n 数量
     * @return 话题ID集合
     */
    private Set<Long> getTopTopicIds(int n) {
        List<Topic> topTopics = topicService.getGlobalHotRank(n);
        return topTopics.stream()
            .map(Topic::getId)
            .collect(Collectors.toSet());
    }

    /**
     * 检测上榜事件
     * <p>
     * 话题首次进入TOP N时触发通知（模拟）
     *
     * @param previousTopIds 之前TOP N的话题ID集合
     * @param n TOP N阈值
     */
    private void checkTopNEvent(Set<Long> previousTopIds, int n) {
        Set<Long> currentTopIds = getTopTopicIds(n);

        // 找出新上榜的话题
        Set<Long> newTopIds = new HashSet<>(currentTopIds);
        newTopIds.removeAll(previousTopIds);

        if (!newTopIds.isEmpty()) {
            for (Long topicId : newTopIds) {
                Topic topic = topicService.getById(topicId);
                if (topic != null) {
                    // 触发上榜事件（模拟通知队列）
                    log.info("【上榜事件触发】话题首次进入TOP{}：topicId={}, title={}, score={}",
                             n, topicId, topic.getTitle(), topic.getCurrentScore());

                    // TODO: 实际项目中这里应该发送到消息队列或通知服务
                    // notificationService.sendTopNAlert(topic);
                }
            }
        }
    }
}
