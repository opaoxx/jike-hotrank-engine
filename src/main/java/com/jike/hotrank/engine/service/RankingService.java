package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.dto.RankingItemDTO;
import com.jike.hotrank.engine.dto.RankingResponseDTO;
import com.jike.hotrank.engine.entity.Circle;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.entity.UserCirclePreference;
import com.jike.hotrank.engine.util.RankingLimits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final TopicService topicService;
    private final CircleService circleService;
    private final UserCirclePreferenceService userCirclePreferenceService;
    private final InteractionEventService interactionEventService;

    public RankingResponseDTO getGlobalRanking(Integer limit) {
        int actualLimit = RankingLimits.global(limit);
        List<Topic> topics = topicService.getGlobalHotRank(actualLimit);
        List<RankingItemDTO> items = convertToRankingItems(topics, null);
        log.debug("Query global ranking: size={}", items.size());
        return RankingResponseDTO.ofGlobal(items);
    }

    public RankingResponseDTO getCircleRanking(Long circleId, Integer limit) {
        int actualLimit = RankingLimits.circle(limit);
        List<Topic> topics = topicService.getCircleHotRank(circleId, actualLimit);
        Circle circle = circleService.getById(circleId);
        String circleName = circle != null ? circle.getName() : "未知圈子";
        List<RankingItemDTO> items = convertToRankingItems(topics, circleName);
        log.debug("Query circle ranking: circleId={}, size={}", circleId, items.size());
        return RankingResponseDTO.ofCircle(circleId, circleName, items);
    }

    public RankingResponseDTO getNewcomerRanking(Integer limit) {
        int actualLimit = RankingLimits.newcomer(limit);
        List<Topic> topics = topicService.getNewcomerRank(actualLimit);
        List<RankingItemDTO> items = convertToRankingItems(topics, null);
        log.debug("Query newcomer ranking: size={}", items.size());
        return RankingResponseDTO.ofNewcomer(items);
    }

    public RankingResponseDTO getSurgingRanking(Integer limit) {
        int actualLimit = RankingLimits.surging(limit);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);

        Map<Long, InteractionWindowStats> currentStats = toInteractionStatsMap(
            interactionEventService.aggregateWeightedScoreByTopic(oneHourAgo, now));
        Map<Long, InteractionWindowStats> previousStats = toInteractionStatsMap(
            interactionEventService.aggregateWeightedScoreByTopic(twoHoursAgo, oneHourAgo));

        List<Topic> topics = currentStats.entrySet().stream()
            .filter(entry -> entry.getValue().weightedScore().compareTo(BigDecimal.ZERO) > 0)
            .map(entry -> toSurgingTopic(entry.getKey(), entry.getValue(), previousStats.get(entry.getKey())))
            .filter(Objects::nonNull)
            .sorted(Comparator
                .comparing(Topic::getCurrentScore, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(Topic::getInteractionCount, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(actualLimit)
            .collect(Collectors.toList());

        List<RankingItemDTO> items = convertToRankingItems(topics, null);
        log.debug("Query surging ranking: size={}", items.size());
        return RankingResponseDTO.ofSurging(items);
    }

    public RankingResponseDTO getPersonalizedGlobalRanking(Long userId, Integer limit) {
        int actualLimit = RankingLimits.personalized(limit);
        List<Topic> topics = topicService.getGlobalHotRank(RankingLimits.personalizedCandidate(limit));

        Map<Long, BigDecimal> preferenceWeightMap = userCirclePreferenceService.getUserPreferences(userId).stream()
            .collect(Collectors.toMap(
                UserCirclePreference::getCircleId,
                UserCirclePreference::getWeight,
                (existing, replacement) -> existing
            ));

        List<Topic> personalizedTopics = topics.stream()
            .map(topic -> toPersonalizedTopic(topic, preferenceWeightMap))
            .sorted(Comparator.comparing(Topic::getCurrentScore).reversed())
            .limit(actualLimit)
            .collect(Collectors.toList());

        List<RankingItemDTO> items = convertToRankingItems(personalizedTopics, null);
        log.info("Query personalized ranking: userId={}, size={}", userId, items.size());
        return RankingResponseDTO.ofPersonalized(userId, items);
    }

    private Topic toPersonalizedTopic(Topic topic, Map<Long, BigDecimal> preferenceWeightMap) {
        BigDecimal preferenceWeight = preferenceWeightMap.getOrDefault(topic.getCircleId(), BigDecimal.ONE);
        BigDecimal baseScore = topic.getCurrentScore() != null ? topic.getCurrentScore() : BigDecimal.ZERO;

        Topic personalized = new Topic();
        personalized.setId(topic.getId());
        personalized.setCircleId(topic.getCircleId());
        personalized.setTitle(topic.getTitle());
        personalized.setAuthorId(topic.getAuthorId());
        personalized.setCurrentScore(baseScore.multiply(preferenceWeight));
        personalized.setInteractionCount(topic.getInteractionCount());
        personalized.setPublishTime(topic.getPublishTime());
        return personalized;
    }

    private List<RankingItemDTO> convertToRankingItems(List<Topic> topics, String circleName) {
        List<RankingItemDTO> items = new ArrayList<>();
        for (int i = 0; i < topics.size(); i++) {
            Topic topic = topics.get(i);
            RankingItemDTO item = new RankingItemDTO();
            item.setRank(i + 1);
            item.setTopicId(topic.getId());
            item.setTitle(topic.getTitle());
            item.setCircleId(topic.getCircleId());
            item.setCircleName(circleName != null ? circleName : getCircleName(topic.getCircleId()));
            item.setScore(topic.getCurrentScore());
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

    private Map<Long, InteractionWindowStats> toInteractionStatsMap(List<Map<String, Object>> rows) {
        Map<Long, InteractionWindowStats> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Long topicId = getLong(row, "topic_id", "topicId");
            BigDecimal weightedScore = getDecimal(row, "weighted_score", "weightedScore");
            int totalCount = getLong(row, "total_count", "totalCount").intValue();
            result.put(topicId, new InteractionWindowStats(weightedScore, totalCount));
        }
        return result;
    }

    private Topic toSurgingTopic(Long topicId, InteractionWindowStats current, InteractionWindowStats previous) {
        Topic topic = topicService.getById(topicId);
        if (topic == null || topic.getStatus() == null || topic.getStatus() != 1) {
            return null;
        }

        BigDecimal previousScore = previous != null ? previous.weightedScore() : BigDecimal.ZERO;
        BigDecimal surgeScore = previousScore.compareTo(BigDecimal.ZERO) <= 0
            ? current.weightedScore()
            : current.weightedScore().divide(previousScore, 4, RoundingMode.HALF_UP);

        Topic surgingTopic = new Topic();
        surgingTopic.setId(topic.getId());
        surgingTopic.setCircleId(topic.getCircleId());
        surgingTopic.setTitle(topic.getTitle());
        surgingTopic.setAuthorId(topic.getAuthorId());
        surgingTopic.setPublishTime(topic.getPublishTime());
        surgingTopic.setInteractionCount(current.totalCount());
        surgingTopic.setCurrentScore(surgeScore.setScale(4, RoundingMode.HALF_UP));
        return surgingTopic;
    }

    private Long getLong(Map<String, Object> row, String snakeCaseKey, String camelCaseKey) {
        Object value = row.get(snakeCaseKey);
        if (value == null) {
            value = row.get(camelCaseKey);
        }
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Aggregation result missing numeric field: " + snakeCaseKey);
        }
        return number.longValue();
    }

    private BigDecimal getDecimal(Map<String, Object> row, String snakeCaseKey, String camelCaseKey) {
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

    private record InteractionWindowStats(BigDecimal weightedScore, int totalCount) {
    }
}
