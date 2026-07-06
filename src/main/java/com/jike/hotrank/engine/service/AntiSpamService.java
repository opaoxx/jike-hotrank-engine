package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.config.HotRankProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AntiSpamService {

    private static final String RATE_LIMIT_REASON = "rate_limit";
    private static final String DEVICE_PENALTY_REASON = "device_fingerprint_penalty";
    private static final String ALLOW_REASON = "allow";

    private final InteractionEventService interactionEventService;
    private final TopicService topicService;
    private final HotRankProperties hotRankProperties;

    public CheckResult checkInteraction(Long userId, Long topicId, String deviceFingerprint) {
        if (!checkFrequencyLimit(userId, topicId)) {
            log.warn("Interaction rejected by rate limit: userId={}, topicId={}", userId, topicId);
            return CheckResult.deny(RATE_LIMIT_REASON);
        }

        DevicePenaltyResult devicePenalty = detectDevicePenalty(userId, deviceFingerprint);
        if (devicePenalty.hasPenalty()) {
            log.warn("Interaction allowed with device penalty: userId={}, topicId={}, device={}, users={}, multiplier={}",
                userId, topicId, deviceFingerprint, devicePenalty.distinctUserCount(), devicePenalty.multiplier());
            return CheckResult.allow(devicePenalty.multiplier(), DEVICE_PENALTY_REASON);
        }

        return CheckResult.allow(BigDecimal.ONE, ALLOW_REASON);
    }

    public boolean checkFrequencyLimit(Long userId, Long topicId) {
        HotRankProperties.AntiSpam.Frequency frequency = hotRankProperties.getAntiSpam().getFrequency();
        if (!frequency.isEnabled()) {
            return true;
        }

        int count = interactionEventService.countByUserAndTopic(userId, topicId);
        boolean allowed = count < frequency.getMaxInteractions();

        if (!allowed) {
            log.info("Rate limit triggered: userId={}, topicId={}, count={}, limit={}",
                userId, topicId, count, frequency.getMaxInteractions());
        }

        return allowed;
    }

    public boolean checkDeviceFingerprint(String deviceFingerprint) {
        return !detectDevicePenalty(null, deviceFingerprint).hasPenalty();
    }

    public DevicePenaltyResult detectDevicePenalty(Long userId, String deviceFingerprint) {
        HotRankProperties.AntiSpam.Device device = hotRankProperties.getAntiSpam().getDevice();
        if (!device.isEnabled() || deviceFingerprint == null || deviceFingerprint.isBlank()) {
            return DevicePenaltyResult.none(0);
        }

        int distinctUserCount = interactionEventService.countDistinctUserByDevice(deviceFingerprint);
        if (userId != null && !interactionEventService.hasUserUsedDevice(userId, deviceFingerprint)) {
            distinctUserCount++;
        }
        if (distinctUserCount >= device.getSecondPenaltyUserThreshold()) {
            return new DevicePenaltyResult(device.getSecondPenaltyMultiplier(), distinctUserCount);
        }
        if (distinctUserCount >= device.getFirstPenaltyUserThreshold()) {
            return new DevicePenaltyResult(device.getFirstPenaltyMultiplier(), distinctUserCount);
        }
        return DevicePenaltyResult.none(distinctUserCount);
    }

    public boolean checkAnomalySpike(Long topicId) {
        HotRankProperties.AntiSpam.Surge surge = hotRankProperties.getAntiSpam().getSurge();
        if (!surge.isEnabled()) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentStart = now.minusHours(surge.getCurrentWindowHours());
        LocalDateTime historyStart = now.minusHours(surge.getHistoryWindowHours());

        int recentCount = interactionEventService.countByTopicAndTimeRange(topicId, currentStart, now);
        if (recentCount < surge.getMinimumCurrentCount()) {
            return true;
        }

        int historyCount = interactionEventService.countByTopicAndTimeRange(topicId, historyStart, currentStart);
        double historyHours = Math.max(
            1.0,
            surge.getHistoryWindowHours() - surge.getCurrentWindowHours()
        );
        double avgHourlyCount = historyCount / historyHours;
        if (avgHourlyCount <= 0) {
            return true;
        }

        double ratio = recentCount / avgHourlyCount;
        boolean isNormal = ratio <= surge.getMultiplierThreshold();

        if (!isNormal) {
            log.warn("Topic surge detected: topicId={}, recentCount={}, avgHourlyCount={}, ratio={}, threshold={}",
                topicId, recentCount, avgHourlyCount, ratio, surge.getMultiplierThreshold());
            topicService.markForReview(topicId);
        }

        return isNormal;
    }

    public record CheckResult(boolean allowed, BigDecimal weightMultiplier, String reason) {
        public static CheckResult allow(BigDecimal weightMultiplier, String reason) {
            return new CheckResult(true, weightMultiplier, reason);
        }

        public static CheckResult deny(String reason) {
            return new CheckResult(false, BigDecimal.ZERO, reason);
        }
    }

    public record DevicePenaltyResult(BigDecimal multiplier, int distinctUserCount) {
        public static DevicePenaltyResult none(int distinctUserCount) {
            return new DevicePenaltyResult(BigDecimal.ONE, distinctUserCount);
        }

        public boolean hasPenalty() {
            return multiplier.compareTo(BigDecimal.ONE) < 0;
        }
    }
}
