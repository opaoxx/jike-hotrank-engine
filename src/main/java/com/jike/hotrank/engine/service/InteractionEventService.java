package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.config.HotRankProperties;
import com.jike.hotrank.engine.entity.InteractionEvent;
import com.jike.hotrank.engine.mapper.InteractionEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 互动事件服务类
 * 提供互动事件的业务操作
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionEventService {

    private final InteractionEventMapper interactionEventMapper;
    private final HotRankProperties hotRankProperties;

    /**
     * 记录互动事件
     *
     * @param event 互动事件
     * @return 创建后的事件（包含生成的ID）
     */
    public InteractionEvent record(InteractionEvent event) {
        if (event.getWeightMultiplier() == null) {
            event.setWeightMultiplier(BigDecimal.ONE);
        }
        event.setCreatedAt(LocalDateTime.now());
        interactionEventMapper.insert(event);
        log.debug("记录互动事件：topicId={}, userId={}, type={}",
                  event.getTopicId(), event.getUserId(), event.getInteractionType());
        return event;
    }

    /**
     * 批量记录互动事件
     *
     * @param events 互动事件列表
     * @return 插入行数
     */
    public int batchRecord(List<InteractionEvent> events) {
        if (events == null || events.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        events.forEach(e -> {
            e.setCreatedAt(now);
            if (e.getWeightMultiplier() == null) {
                e.setWeightMultiplier(BigDecimal.ONE);
            }
        });
        int rows = interactionEventMapper.batchInsert(events);
        log.info("批量记录互动事件：count={}", events.size());
        return rows;
    }

    /**
     * 查询用户对指定话题的互动次数（24小时内）
     *
     * @param userId 用户ID
     * @param topicId 话题ID
     * @return 互动次数
     */
    public int countByUserAndTopic(Long userId, Long topicId) {
        int windowHours = hotRankProperties.getAntiSpam().getFrequency().getWindowHours();
        LocalDateTime startTime = LocalDateTime.now().minusHours(windowHours);
        return interactionEventMapper.countByUserAndTopic(userId, topicId, startTime);
    }

    /**
     * 按话题聚合互动事件（用于热度计算）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 聚合结果列表
     */
    public List<Map<String, Object>> aggregateByTopic(LocalDateTime startTime, LocalDateTime endTime) {
        return interactionEventMapper.aggregateByTopic(startTime, endTime);
    }

    /**
     * 按话题聚合全部互动事件（用于全量热度重算）
     *
     * @return 聚合结果列表
     */
    public List<Map<String, Object>> aggregateAllByTopic() {
        return interactionEventMapper.aggregateAllByTopic();
    }

    public List<Map<String, Object>> aggregateWeightedScoreAllByTopic() {
        HotRankProperties.Heat.Weights weights = hotRankProperties.getHeat().getWeights();
        return interactionEventMapper.aggregateWeightedScoreAllByTopic(
            weights.getLike(),
            weights.getBookmark(),
            weights.getShare(),
            weights.getComment()
        );
    }

    /**
     * 按话题聚合指定时间段内的加权互动分（用于飙升榜）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 聚合结果列表
     */
    public List<Map<String, Object>> aggregateWeightedScoreByTopic(LocalDateTime startTime, LocalDateTime endTime) {
        HotRankProperties.Heat.Weights weights = hotRankProperties.getHeat().getWeights();
        return interactionEventMapper.aggregateWeightedScoreByTopic(
            startTime,
            endTime,
            weights.getLike(),
            weights.getBookmark(),
            weights.getShare(),
            weights.getComment()
        );
    }

    /**
     * 查询话题在指定时间段内的互动总量
     *
     * @param topicId 话题ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 互动次数
     */
    public int countByTopicAndTimeRange(Long topicId, LocalDateTime startTime, LocalDateTime endTime) {
        return interactionEventMapper.countByTopicAndTimeRange(topicId, startTime, endTime);
    }

    /**
     * 查询设备指纹关联的不同用户数
     *
     * @param deviceFingerprint 设备指纹
     * @return 不同用户数量
     */
    public int countDistinctUserByDevice(String deviceFingerprint) {
        int windowHours = hotRankProperties.getAntiSpam().getDevice().getWindowHours();
        LocalDateTime startTime = LocalDateTime.now().minusHours(windowHours);
        return interactionEventMapper.countDistinctUserByDevice(deviceFingerprint, startTime);
    }

    public boolean hasUserUsedDevice(Long userId, String deviceFingerprint) {
        int windowHours = hotRankProperties.getAntiSpam().getDevice().getWindowHours();
        LocalDateTime startTime = LocalDateTime.now().minusHours(windowHours);
        return interactionEventMapper.countByUserAndDevice(userId, deviceFingerprint, startTime) > 0;
    }
}
