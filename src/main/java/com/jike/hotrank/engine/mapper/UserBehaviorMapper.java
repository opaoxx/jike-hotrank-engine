package com.jike.hotrank.engine.mapper;

import com.jike.hotrank.engine.entity.UserBehavior;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户行为Mapper接口
 */
@Mapper
public interface UserBehaviorMapper {

    /**
     * 插入用户行为记录
     */
    int insert(UserBehavior behavior);

    /**
     * 查询用户对指定话题的有效行为次数（24小时内）
     * @param userId 用户ID
     * @param topicId 话题ID
     * @param startTime 窗口开始时间
     * @return 有效行为次数
     */
    int countValidByUserAndTopic(@Param("userId") Long userId, @Param("topicId") Long topicId,
                                 @Param("startTime") LocalDateTime startTime);

    /**
     * 查询设备指纹关联的不同用户数
     * @param deviceFingerprint 设备指纹
     * @param startTime 开始时间
     * @return 不同用户数量
     */
    int countDistinctUserByDevice(@Param("deviceFingerprint") String deviceFingerprint,
                                  @Param("startTime") LocalDateTime startTime);

    /**
     * 标记行为为无效
     * @param id 行为ID
     * @param reason 无效原因
     * @return 更新行数
     */
    int markInvalid(@Param("id") Long id, @Param("reason") String reason);

    /**
     * 查询异常行为列表（用于防刷报告）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 异常行为列表
     */
    List<UserBehavior> selectInvalidBehaviors(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
}
