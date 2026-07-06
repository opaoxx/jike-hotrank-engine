package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.entity.UserCirclePreference;
import com.jike.hotrank.engine.mapper.UserCirclePreferenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户圈子偏好服务类
 * 用于个性化榜单重排
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCirclePreferenceService {

    private final UserCirclePreferenceMapper userCirclePreferenceMapper;

    /**
     * 获取用户的圈子偏好列表
     *
     * @param userId 用户ID
     * @return 偏好列表（按权重降序）
     */
    public List<UserCirclePreference> getUserPreferences(Long userId) {
        return userCirclePreferenceMapper.selectByUserId(userId);
    }

    /**
     * 获取用户对指定圈子的偏好权重
     *
     * @param userId 用户ID
     * @param circleId 圈子ID
     * @return 偏好权重，如果不存在返回默认值1.0
     */
    public BigDecimal getPreferenceWeight(Long userId, Long circleId) {
        UserCirclePreference preference = userCirclePreferenceMapper.selectByUserAndCircle(userId, circleId);
        return preference != null ? preference.getWeight() : BigDecimal.ONE;
    }

    /**
     * 更新用户圈子偏好
     * <p>
     * 当用户与某个圈子产生互动时调用，自动增加互动次数并调整权重
     *
     * @param userId 用户ID
     * @param circleId 圈子ID
     */
    public void updatePreference(Long userId, Long circleId) {
        // 计算新权重：基于互动次数的对数函数
        UserCirclePreference existing = userCirclePreferenceMapper.selectByUserAndCircle(userId, circleId);

        if (existing == null) {
            // 新建偏好记录
            UserCirclePreference newPreference = new UserCirclePreference();
            newPreference.setUserId(userId);
            newPreference.setCircleId(circleId);
            newPreference.setWeight(BigDecimal.ONE);
            newPreference.setInteractionCount(1);
            userCirclePreferenceMapper.insertOrUpdate(newPreference);
            log.debug("新建用户圈子偏好：userId={}, circleId={}", userId, circleId);
        } else {
            // 增加互动次数
            userCirclePreferenceMapper.incrementInteractionCount(userId, circleId);

            // 根据互动次数计算新权重：weight = 1 + log2(interactionCount)
            int newCount = existing.getInteractionCount() + 1;
            double newWeight = 1 + Math.log(newCount) / Math.log(2);
            BigDecimal weight = BigDecimal.valueOf(Math.min(newWeight, 10.0)); // 最大权重10

            UserCirclePreference update = new UserCirclePreference();
            update.setUserId(userId);
            update.setCircleId(circleId);
            update.setWeight(weight);
            update.setInteractionCount(newCount);
            userCirclePreferenceMapper.insertOrUpdate(update);
            log.debug("更新用户圈子偏好：userId={}, circleId={}, weight={}", userId, circleId, weight);
        }
    }
}
