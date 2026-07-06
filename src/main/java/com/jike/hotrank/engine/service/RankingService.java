package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.dto.RankingItemDTO;
import com.jike.hotrank.engine.dto.RankingResponseDTO;
import com.jike.hotrank.engine.entity.Circle;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.entity.UserCirclePreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        int actualLimit = (limit != null && limit > 0) ? limit : GLOBAL_RANK_LIMIT;
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
        int actualLimit = (limit != null && limit > 0) ? limit : CIRCLE_RANK_LIMIT;
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
        int actualLimit = (limit != null && limit > 0) ? limit : NEWCOMER_RANK_LIMIT;
        List<Topic> topics = topicService.getNewcomerRank(actualLimit);
        List<RankingItemDTO> items = convertToRankingItems(topics, null);
        log.debug("查询新星榜：size={}", items.size());
        return RankingResponseDTO.ofNewcomer(items);
    }

    /**
     * 获取飙升榜（近1小时热度增速最快的话题）
     * <p>
     * 算法：用当前热度分 / 上一小时热度分 计算增速比
     * 注意：简化实现，使用热度分作为排序依据
     *
     * @param limit 返回数量限制（可选，默认10）
     * @return 飙升榜响应
     */
    public RankingResponseDTO getSurgingRanking(Integer limit) {
        int actualLimit = (limit != null && limit > 0) ? limit : SURGING_RANK_LIMIT;

        // 简化实现：获取全站热榜TOP N作为飙升榜
        // 完整实现需要对比当前小时和上一小时的热度分增量
        List<Topic> topics = topicService.getGlobalHotRank(actualLimit);
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
        int actualLimit = (limit != null && limit > 0) ? limit : GLOBAL_RANK_LIMIT;

        // 获取更多的话题以便重排（获取2倍数量）
        List<Topic> topics = topicService.getGlobalHotRank(actualLimit * 2);

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
        return RankingResponseDTO.ofGlobal(items);
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
}
