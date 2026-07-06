package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.entity.InteractionEvent;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 互动事件控制器
 * 提供互动事件写入接口
 * <p>
 * 集成防刷机制：
 * 1. 滑动窗口频率限制
 * 2. 设备指纹聚合检测
 * 3. 异常突增检测
 * <p>
 * 集成个性化榜单：
 * 互动时自动更新用户圈子偏好
 *
 * @author JikeHotRank Team
 */
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

    /**
     * 记录互动事件
     * <p>
     * 用户的每次点赞/评论/转发/收藏触发一条事件记录入库
     * <p>
     * 防刷机制：
     * 1. 频率限制：同一用户对同一话题24小时内互动上限
     * 2. 设备指纹检测：相同设备指纹多账号不叠加
     * 3. 异常突增检测：1小时内互动量超历史均值10倍触发审核
     * <p>
     * 个性化榜单：
     * 互动时自动更新用户圈子偏好权重
     *
     * @param event 互动事件信息
     * @return 创建后的事件
     */
    @PostMapping
    public ApiResponse<InteractionEvent> recordInteraction(@RequestBody InteractionEvent event) {
        log.info("收到互动事件：topicId={}, userId={}, type={}",
                 event.getTopicId(), event.getUserId(), event.getInteractionType());

        // 参数校验
        if (event.getTopicId() == null || event.getUserId() == null || event.getInteractionType() == null) {
            return ApiResponse.error(400, "参数不完整：topicId、userId、interactionType不能为空");
        }

        // 互动类型校验
        if (event.getInteractionType() != 1 && event.getInteractionType() != 2 &&
            event.getInteractionType() != 3 && event.getInteractionType() != 5) {
            return ApiResponse.error(400, "互动类型无效：必须为1(点赞)、2(收藏)、3(转发)或5(评论)");
        }

        // 检查话题是否存在且状态正常
        Topic topic = topicService.getById(event.getTopicId());
        if (topic == null) {
            return ApiResponse.error(404, "话题不存在");
        }
        if (topic.getStatus() == 0) {
            return ApiResponse.error(403, "话题已被屏蔽");
        }

        // 防刷校验
        boolean isValid = antiSpamService.checkInteraction(
            event.getUserId(), event.getTopicId(), event.getDeviceFingerprint());

        if (!isValid) {
            // 记录无效行为
            userBehaviorService.recordInvalid(
                event.getUserId(), event.getTopicId(), event.getInteractionType(),
                event.getDeviceFingerprint(), event.getIpAddress(), "防刷机制拦截");
            return ApiResponse.error(429, "互动频率过高，请稍后再试");
        }

        // 记录有效行为
        userBehaviorService.recordValid(
            event.getUserId(), event.getTopicId(), event.getInteractionType(),
            event.getDeviceFingerprint(), event.getIpAddress());

        // 记录互动事件
        InteractionEvent recorded = interactionEventService.record(event);

        // 更新用户圈子偏好（用于个性化榜单）
        try {
            userCirclePreferenceService.updatePreference(event.getUserId(), topic.getCircleId());
        } catch (Exception e) {
            log.warn("更新用户圈子偏好失败：userId={}, circleId={}", event.getUserId(), topic.getCircleId(), e);
        }

        // 检测异常突增
        antiSpamService.checkAnomalySpike(event.getTopicId());

        return ApiResponse.success(recorded);
    }
}
