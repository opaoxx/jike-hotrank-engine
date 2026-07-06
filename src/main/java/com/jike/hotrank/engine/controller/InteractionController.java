package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.entity.InteractionEvent;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.AntiSpamService;
import com.jike.hotrank.engine.service.InteractionEventService;
import com.jike.hotrank.engine.service.TopicService;
import com.jike.hotrank.engine.service.UserBehaviorService;
import com.jike.hotrank.engine.service.UserCirclePreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionEventService interactionEventService;
    private final AntiSpamService antiSpamService;
    private final UserBehaviorService userBehaviorService;
    private final TopicService topicService;
    private final UserCirclePreferenceService userCirclePreferenceService;

    @PostMapping
    public ApiResponse<InteractionEvent> recordInteraction(@RequestBody InteractionEvent event) {
        log.info("Received interaction: topicId={}, userId={}, type={}",
            event.getTopicId(), event.getUserId(), event.getInteractionType());

        if (event.getTopicId() == null || event.getUserId() == null || event.getInteractionType() == null) {
            return ApiResponse.error(400, "参数不完整：topicId、userId、interactionType 不能为空");
        }

        if (!isSupportedInteractionType(event.getInteractionType())) {
            return ApiResponse.error(400, "互动类型无效：必须为 1(点赞)、2(收藏)、3(转发) 或 5(评论)");
        }

        Topic topic = topicService.getById(event.getTopicId());
        if (topic == null) {
            return ApiResponse.error(404, "话题不存在");
        }
        if (topic.getStatus() == 0) {
            return ApiResponse.error(403, "话题已被屏蔽");
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
            return ApiResponse.error(429, "互动频率过高，请稍后再试");
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

        try {
            userCirclePreferenceService.updatePreference(event.getUserId(), topic.getCircleId());
        } catch (Exception e) {
            log.warn("Failed to update user circle preference: userId={}, circleId={}",
                event.getUserId(), topic.getCircleId(), e);
        }

        antiSpamService.checkAnomalySpike(event.getTopicId());

        return ApiResponse.success(recorded);
    }

    private boolean isSupportedInteractionType(Integer interactionType) {
        return interactionType == 1 || interactionType == 2 || interactionType == 3 || interactionType == 5;
    }
}
