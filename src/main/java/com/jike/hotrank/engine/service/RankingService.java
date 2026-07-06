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

/**
 * 榜单服务类
 * 提供四类榜单查询：全站热榜、圈子热榜、新星榜、飙升榜
 * 支持个性化榜单重排
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final TopicService topicService;
    private final CircleService circleService;
    private final UserCirclePreferenceService userCirclePreferenceService;
    private final InteractionEventService interactionEventService;

    /** 全站热榜默认数量 */
    private static final int GLOBAL_RANK_LIMIT = 50;

    /** 圈子热榜默认数量 */
    private static final int CIRCLE_RANK_LIMIT = 20;

    /** 新星榜默认数量 */
    private static final int NEWCOMER_RANK_LIMIT = 10;

    /** 飙升榜默认数量 */
    private static final int SURGING_RANK_LIMIT = 10;

    /**
     * 获取全站热榜
     *
     * @param limit 返回数量限制（可选，默认50）
     * @return 全站热榜响应
     */
    public RankingResponseDTO getGlobalRanking(Integer limit) {
        int actualLimit = RankingLimits.global(limit);
        List<Topic> topics = topicService.getGlobalHotRank(actualLimit);
        List<RankingItemDTO> items = convertToRankingItems(topics, null);
        log.debug("查询全站热榜：size={}", items.size());
        return RankingResponseDTO.ofGlobal(items);
    }

    /**
     * 获取圈子热榜
     *
     * @param circleId 圈子ID
     * @param limit 返回数量限制（可选，默认20）
     * @return 圈子热榜响应
     */
    public RankingResponseDTO getCircleRanking(Long circleId, Integer limit) {
        int actualLimit = RankingLimits.circle(limit);
        List<Topic> topics = topicService.getCircleHotRank(circleId, actualLimit);

        // 获取圈子名称
        Circle circle = circleService.getById(circleId);
        String circleName = (circle != null) ? circle.getName() : "未知圈子";

        List<RankingItemDTO> items = convertToRankingItems(topics, circleName);
        log.debug("查询圈子热榜：circleId={}, size={}", circleId, items.size());
        return RankingResponseDTO.ofCircle(circleId, circleName, items);
    }

    /**
     * 获取新星榜（24小时内发布的热门话题）
     *
     * @param limit 返回数量限制（可选，默认10）
     * @return 新星榜响应
     */
    public RankingResponseDTO getNewcomerRanking(Integer limit) {
        int actualLimit = RankingLimits.newcomer(limit);
        List<Topic> topics = topicService.getNewcomerRank(actualLimit);
        List<RankingItemDTO> items = convertToRankingItems(topics, null);
        log.debug("查询新星榜：size={}", items.size());
        return RankingResponseDTO.ofNewcomer(items);
    }

    /**
     * 获取飙升榜（近1小时热度增速最快的话题）
     * <p>
     * 算法：用当前1小时加权互动增量 / 上一小时加权互动增量 计算增速比
     *
     * @param limit 返回数量限制（可选，默认10）
     * @return 飙升榜响应
     */
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
        log.debug("查询飙升榜：size={}", items.size());
        return RankingResponseDTO.ofSurging(items);
    }

    /**
     * 获取个性化全站热榜
     * <p>
     * 根据用户的圈子偏好权重对榜单进行重排
     *
     * @param userId 用户ID
     * @param limit 返回数量限制（可选，默认50）
     * @return 个性化全站热榜响应
     */
    public RankingResponseDTO getPersonalizedGlobalRanking(Long userId, Integer limit) {
        int actualLimit = RankingLimits.personalized(limit);

        // 获取更多的话题以便重排（获取2倍数量）
        List<Topic> topics = topicService.getGlobalHotRank(RankingLimits.personalizedCandidate(limit));

        // 获取用户圈子偏好
        List<UserCirclePreference> preferences = userCirclePreferenceService.getUserPreferences(userId);
        Map<Long, BigDecimal> preferenceWeightMap = preferences.stream()
            .collect(Collectors.toMap(
                UserCirclePreference::getCircleId,
                UserCirclePreference::getWeight,
                (existing, replacement) -> existing
            ));

        // 计算个性化分数并重排
        List<Topic> personalizedTopics = topics.stream()
            .map(topic -> {
                BigDecimal preferenceWeight = preferenceWeightMap.getOrDefault(topic.getCircleId(), BigDecimal.ONE);
                // 个性化分数 = 原始热度分 * 偏好权重
                BigDecimal personalizedScore = topic.getCurrentScore().multiply(preferenceWeight);
                Topic personalized = new Topic();
                personalized.setId(topic.getId());
                personalized.setCircleId(topic.getCircleId());
                personalized.setTitle(topic.getTitle());
                personalized.setAuthorId(topic.getAuthorId());
                personalized.setCurrentScore(personalizedScore);
                personalized.setInteractionCount(topic.getInteractionCount());
                personalized.setPublishTime(topic.getPublishTime());
                return personalized;
            })
            .sorted(Comparator.comparing(Topic::getCurrentScore).reversed())
            .limit(actualLimit)
            .collect(Collectors.toList());

        List<RankingItemDTO> items = convertToRankingItems(personalizedTopics, null);
        log.info("查询个性化热榜：userId={}, size={}", userId, items.size());
        return RankingResponseDTO.ofPersonalized(userId, items);
    }

    /**
     * 将话题列表转换为排名项列表
     *
     * @param topics 话题列表
     * @param circleName 圈子名称（可选，用于圈子热榜）
     * @return 排名项列表
     */
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

    /**
     * 获取圈子名称
     */
    private String getCircleName(Long circleId) {
        if (circleId == null) {
            return "未知圈子";
        }
        Circle circle = circleService.getById(circleId);
        return (circle != null) ? circle.getName() : "未知圈子";
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
            throw new IllegalStateException("聚合结果缺少数值字段：" + snakeCaseKey);
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
        throw new IllegalStateException("鑱氬悎缁撴灉缂哄皯鏁板€煎瓧娈碉細" + snakeCaseKey);
    }

    private record InteractionWindowStats(BigDecimal weightedScore, int totalCount) {
    }
}
