package com.jike.hotrank.engine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 防刷服务类
 * <p>
 * 提供三种防刷机制：
 * 1. 滑动窗口频率限制：同一用户对同一话题24小时内有效互动上限
 * 2. 设备指纹聚合检测：相同设备指纹的多个账号互动不叠加计算
 * 3. 异常突增检测：某话题1小时内互动量超过历史均值10倍，自动触发待审核标记
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AntiSpamService {

    private final InteractionEventService interactionEventService;
    private final TopicService topicService;

    /** 用户对同一话题24小时内最大互动次数 */
    private static final int MAX_INTERACTIONS_PER_TOPIC_24H = 10;

    /** 设备指纹关联用户数阈值（超过此值视为异常） */
    private static final int DEVICE_USER_THRESHOLD = 5;

    /** 异常突增倍数阈值 */
    private static final int ANOMALY_SPIKE_MULTIPLIER = 10;

    /**
     * 检查互动是否有效（防刷校验）
     *
     * @param userId 用户ID
     * @param topicId 话题ID
     * @param deviceFingerprint 设备指纹
     * @return 校验结果：true-有效，false-被拦截
     */
    public boolean checkInteraction(Long userId, Long topicId, String deviceFingerprint) {
        // 1. 滑动窗口频率限制
        if (!checkFrequencyLimit(userId, topicId)) {
            log.warn("互动被拦截-频率超限：userId={}, topicId={}", userId, topicId);
            return false;
        }

        // 2. 设备指纹聚合检测
        if (deviceFingerprint != null && !checkDeviceFingerprint(deviceFingerprint)) {
            log.warn("互动被拦截-设备指纹异常：userId={}, deviceFingerprint={}", userId, deviceFingerprint);
            return false;
        }

        return true;
    }

    /**
     * 滑动窗口频率限制检查
     * <p>
     * 同一用户对同一话题24小时内有效互动上限
     *
     * @param userId 用户ID
     * @param topicId 话题ID
     * @return true-未超限，false-已超限
     */
    public boolean checkFrequencyLimit(Long userId, Long topicId) {
        int count = interactionEventService.countByUserAndTopic(userId, topicId);
        boolean isValid = count < MAX_INTERACTIONS_PER_TOPIC_24H;

        if (!isValid) {
            log.info("频率限制触发：userId={}, topicId={}, 24h内互动次数={}", userId, topicId, count);
        }

        return isValid;
    }

    /**
     * 设备指纹聚合检测
     * <p>
     * 相同设备指纹的多个账号互动不叠加计算
     * 如果同一设备指纹关联的用户数超过阈值，视为异常
     *
     * @param deviceFingerprint 设备指纹
     * @return true-正常，false-异常
     */
    public boolean checkDeviceFingerprint(String deviceFingerprint) {
        if (deviceFingerprint == null || deviceFingerprint.isEmpty()) {
            return true;
        }

        int distinctUserCount = interactionEventService.countDistinctUserByDevice(deviceFingerprint);
        boolean isNormal = distinctUserCount < DEVICE_USER_THRESHOLD;

        if (!isNormal) {
            log.info("设备指纹异常：deviceFingerprint={}, 关联用户数={}", deviceFingerprint, distinctUserCount);
        }

        return isNormal;
    }

    /**
     * 检测话题互动异常突增
     * <p>
     * 某话题1小时内互动量超过历史均值10倍，自动触发待审核标记
     *
     * @param topicId 话题ID
     * @return true-正常，false-存在异常突增
     */
    public boolean checkAnomalySpike(Long topicId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        // 查询最近1小时互动量
        int recentCount = interactionEventService.countByTopicAndTimeRange(topicId, oneHourAgo, now);

        // 查询过去24小时平均互动量（每小时）
        int totalCount24h = interactionEventService.countByTopicAndTimeRange(topicId, twentyFourHoursAgo, now);
        double avgHourlyCount = totalCount24h / 24.0;

        // 如果平均值为0，使用阈值10
        double threshold = Math.max(avgHourlyCount * ANOMALY_SPIKE_MULTIPLIER, 10);

        boolean isNormal = recentCount <= threshold;

        if (!isNormal) {
            log.warn("检测到话题互动异常突增：topicId={}, 最近1小时互动量={}, 24小时平均={}, 阈值={}",
                     topicId, recentCount, avgHourlyCount, threshold);

            // 标记话题为待审核
            topicService.markForReview(topicId);
        }

        return isNormal;
    }
}
