package com.jike.hotrank.engine.mapper;

import com.jike.hotrank.engine.entity.UserCirclePreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户圈子偏好Mapper接口
 */
@Mapper
public interface UserCirclePreferenceMapper {

    /**
     * 查询用户的圈子偏好列表（按权重降序）
     * @param userId 用户ID
     * @return 偏好列表
     */
    List<UserCirclePreference> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户对指定圈子的偏好
     * @param userId 用户ID
     * @param circleId 圈子ID
     * @return 偏好信息
     */
    UserCirclePreference selectByUserAndCircle(@Param("userId") Long userId, @Param("circleId") Long circleId);

    /**
     * 插入或更新用户圈子偏好（使用ON DUPLICATE KEY UPDATE）
     * @param preference 偏好信息
     * @return 影响行数
     */
    int insertOrUpdate(UserCirclePreference preference);

    /**
     * 增加用户圈子互动次数
     * @param userId 用户ID
     * @param circleId 圈子ID
     * @return 影响行数
     */
    int incrementInteractionCount(@Param("userId") Long userId, @Param("circleId") Long circleId);
}
