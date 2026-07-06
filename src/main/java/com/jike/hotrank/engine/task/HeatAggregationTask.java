package com.jike.hotrank.engine.task;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.InteractionEventService;
import com.jike.hotrank.engine.service.RankingNotificationService;
import com.jike.hotrank.engine.service.TaskLockService;
import com.jike.hotrank.engine.service.TopicService;
import com.jike.hotrank.engine.util.HeatScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeatAggregationTask {

    private static final int TOP_N_THRESHOLD = 10;

    private final InteractionEventService interactionEventService;
    private final TopicService topicService;
    private final RankingCacheManager cacheManager;
    private final RankingNotificationService rankingNotificationService;
    private final TaskLockService taskLockService;

    @Scheduled(fixedRate = 300000)
    public void aggregateHeatWithLock() {
        taskLockService.runWithLock("jike-hotrank:heat-aggregation", this::aggregateHeat);
    }

    public void aggregateHeat() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(5);

        log.info("Start heat aggregation: startTime={}, endTime={}", startTime, endTime);

        try {
            Set<Long> previousTopTopicIds = getTopTopicIds(TOP_N_THRESHOLD);
            List<Map<String, Object>> aggregationData = interactionEventService.aggregateWeightedScoreAllByTopic();

            if (aggregationData == null || aggregationData.isEmpty()) {
                log.info("Heat aggregation finished: no interaction events");
                return;
            }

            List<Topic> topicsToUpdate = new ArrayList<>();
            for (Map<String, Object> row : aggregationData) {
                Long topicId = getRequiredLong(row, "topic_id", "topicId");
                BigDecimal weightedScore = getRequiredDecimal(row, "weighted_score", "weightedScore");
                int totalInteractionCount = getRequiredLong(row, "total_count", "totalCount").intValue();

                Topic topic = topicService.getById(topicId);
                if (topic == null || topic.getStatus() == null || topic.getStatus() != 1) {
                    log.warn("Skip missing or inactive topic: topicId={}", topicId);
                    continue;
                }

                BigDecimal newScore = HeatScoreCalculator.calculateScore(weightedScore, topic.getPublishTime());

                Topic updateTopic = new Topic();
                updateTopic.setId(topicId);
                updateTopic.setCurrentScore(newScore);
                updateTopic.setInteractionCount(totalInteractionCount);
                topicsToUpdate.add(updateTopic);
            }

            if (!topicsToUpdate.isEmpty()) {
                topicService.batchUpdateScore(topicsToUpdate);
                cacheManager.evictByPrefix(RankingCacheManager.rankingPrefix());
                rankingNotificationService.publishRankingUpdated(topicsToUpdate.size());
                log.info("Heat aggregation finished: updatedTopics={}", topicsToUpdate.size());
                checkTopNEvent(previousTopTopicIds, TOP_N_THRESHOLD);
            }
        } catch (Exception e) {
            log.error("Heat aggregation failed", e);
        }
    }

    private Set<Long> getTopTopicIds(int n) {
        List<Topic> topTopics = topicService.getGlobalHotRank(n);
        return topTopics.stream()
            .map(Topic::getId)
            .collect(Collectors.toSet());
    }

    private void checkTopNEvent(Set<Long> previousTopIds, int n) {
        Set<Long> currentTopIds = getTopTopicIds(n);
        Set<Long> newTopIds = new HashSet<>(currentTopIds);
        newTopIds.removeAll(previousTopIds);

        for (Long topicId : newTopIds) {
            Topic topic = topicService.getById(topicId);
            if (topic != null) {
                publishTopNAlert(n, topic);
            }
        }
    }

    private void publishTopNAlert(int threshold, Topic topic) {
        rankingNotificationService.publishTopNEntered(threshold, topic);
        log.info("Topic entered TOP{}: topicId={}, title={}, score={}",
            threshold, topic.getId(), topic.getTitle(), topic.getCurrentScore());
    }

    private Long getRequiredLong(Map<String, Object> row, String snakeCaseKey, String camelCaseKey) {
        Object value = row.get(snakeCaseKey);
        if (value == null) {
            value = row.get(camelCaseKey);
        }
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Aggregation result missing numeric field: " + snakeCaseKey);
        }
        return number.longValue();
    }

    private BigDecimal getRequiredDecimal(Map<String, Object> row, String snakeCaseKey, String camelCaseKey) {
        Object value = row.get(snakeCaseKey);
        if (value == null) {
            value = row.get(camelCaseKey);
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        throw new IllegalStateException("Aggregation result missing numeric field: " + snakeCaseKey);
    }
}
