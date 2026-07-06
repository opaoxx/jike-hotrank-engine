package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.entity.InteractionEvent;
import com.jike.hotrank.engine.entity.Topic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InteractionWriteService {

    private final InteractionEventService interactionEventService;
    private final AntiSpamService antiSpamService;
    private final UserBehaviorService userBehaviorService;
    private final TopicService topicService;
    private final UserCirclePreferenceService userCirclePreferenceService;
    private final RankingCacheManager cacheManager;

    @Transactional
    public RecordResult recordInteraction(InteractionEvent event) {
        Topic topic = topicService.getById(event.getTopicId());
        if (topic == null) {
            return RecordResult.rejected(404, "话题不存在");
        }
        if (topic.getStatus() == 0) {
            return RecordResult.rejected(403, "话题已被屏蔽");
        }

        AntiSpamService.CheckResult antiSpamResult = antiSpamService.checkInteraction(
            event.getUserId(),
            event.getTopicId(),
            event.getDeviceFingerprint()
        );

        if (!antiSpamResult.allowed()) {
            userBehaviorService.recordInvalid(
                event.getUserId(),
                event.getTopicId(),
                event.getInteractionType(),
                event.getDeviceFingerprint(),
                event.getIpAddress(),
                antiSpamResult.reason()
            );
            return RecordResult.rejected(429, "互动频率过高，请稍后再试");
        }

        event.setWeightMultiplier(antiSpamResult.weightMultiplier());
        userBehaviorService.recordValid(
            event.getUserId(),
            event.getTopicId(),
            event.getInteractionType(),
            event.getDeviceFingerprint(),
            event.getIpAddress()
        );

        InteractionEvent recorded = interactionEventService.record(event);
        userCirclePreferenceService.updatePreference(event.getUserId(), topic.getCircleId());
        cacheManager.evictByPrefix(RankingCacheManager.personalizedRankPrefix(event.getUserId()));
        antiSpamService.checkAnomalySpike(event.getTopicId());

        return RecordResult.accepted(recorded);
    }

    public record RecordResult(boolean accepted, int code, String message, InteractionEvent event) {
        public static RecordResult accepted(InteractionEvent event) {
            return new RecordResult(true, 0, "success", event);
        }

        public static RecordResult rejected(int code, String message) {
            return new RecordResult(false, code, message, null);
        }
    }
}
