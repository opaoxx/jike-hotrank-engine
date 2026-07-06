package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.entity.UserBehavior;
import com.jike.hotrank.engine.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户行为服务类
 * 记录用户互动行为，用于防刷检测
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBehaviorService {

    private final UserBehaviorMapper userBehaviorMapper;

    /**
     * 记录用户行为
     *
     * @param behavior 用户行为
     * @return 创建后的行为（包含生成的ID）
     */
    public UserBehavior record(UserBehavior behavior) {
        behavior.setCreatedAt(LocalDateTime.now());
        userBehaviorMapper.insert(behavior);
        log.debug("记录用户行为：userId={}, topicId={}, type={}, isValid={}",
                  behavior.getUserId(), behavior.getTopicId(),
                  behavior.getInteractionType(), behavior.getIsValid());
        return behavior;
    }

    /**
     * 记录有效用户行为
     *
     * @param userId 用户ID
     * @param topicId 话题ID
     * @param interactionType 互动类型
     * @param deviceFingerprint 设备指纹
     * @param ipAddress IP地址
     * @return 创建后的行为
     */
    public UserBehavior recordValid(Long userId, Long topicId, Integer interactionType,
                                    String deviceFingerprint, String ipAddress) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setTopicId(topicId);
        behavior.setInteractionType(interactionType);
        behavior.setDeviceFingerprint(deviceFingerprint);
        behavior.setIpAddress(ipAddress);
        behavior.setIsValid(1);
        return record(behavior);
    }

    /**
     * 记录无效用户行为（被拦截）
     *
     * @param userId 用户ID
     * @param topicId 话题ID
     * @param interactionType 互动类型
     * @param deviceFingerprint 设备指纹
     * @param ipAddress IP地址
     * @param reason 拦截原因
     * @return 创建后的行为
     */
    public UserBehavior recordInvalid(Long userId, Long topicId, Integer interactionType,
                                      String deviceFingerprint, String ipAddress, String reason) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setTopicId(topicId);
        behavior.setInteractionType(interactionType);
        behavior.setDeviceFingerprint(deviceFingerprint);
        behavior.setIpAddress(ipAddress);
        behavior.setIsValid(0);
        behavior.setInvalidReason(reason);
        return record(behavior);
    }

    /**
     * 查询异常行为列表（用于防刷报告）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常行为列表
     */
    public List<UserBehavior> getInvalidBehaviors(LocalDateTime startTime, LocalDateTime endTime) {
        return userBehaviorMapper.selectInvalidBehaviors(startTime, endTime);
    }

    /**
     * 生成防刷检测报告
     *
     * @return 报告内容
     */
    public String generateAntiSpamReport() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();

        List<UserBehavior> invalidBehaviors = getInvalidBehaviors(todayStart, now);

        StringBuilder report = new StringBuilder();
        report.append("========== 防刷检测报告 ==========\n");
        report.append("报告时间：").append(now).append("\n");
        report.append("统计周期：").append(todayStart).append(" 至 ").append(now).append("\n");
        report.append("当日拦截异常互动数量：").append(invalidBehaviors.size()).append("\n\n");

        if (!invalidBehaviors.isEmpty()) {
            report.append("详细拦截记录：\n");
            for (UserBehavior behavior : invalidBehaviors) {
                report.append("- 用户")
                    .append(behavior.getUserId())
                    .append(" 对话题")
                    .append(behavior.getTopicId())
                    .append(" 的")
                    .append(getInteractionTypeName(behavior.getInteractionType()))
                    .append("互动被拦截，原因：")
                    .append(behavior.getInvalidReason())
                    .append('\n');
            }
        }

        return report.toString();
    }

    /**
     * 获取互动类型名称
     */
    private String getInteractionTypeName(int interactionType) {
        return switch (interactionType) {
            case 1 -> "点赞";
            case 2 -> "收藏";
            case 3 -> "转发";
            case 5 -> "评论";
            default -> "未知";
        };
    }
}
