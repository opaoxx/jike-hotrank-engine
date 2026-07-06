package com.jike.hotrank.engine.mapper;

import com.jike.hotrank.engine.entity.InteractionEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 互动事件Mapper接口
 */
@Mapper
public interface InteractionEventMapper {

    /**
     * 插入互动事件
     */
    int insert(InteractionEvent event);

    /**
     * 批量插入互动事件
     */
    int batchInsert(@Param("list") List<InteractionEvent> events);

    /**
     * 查询用户对指定话题的互动次数（滑动窗口内）
     * @param userId 用户ID
     * @param topicId 话题ID
     * @param startTime 窗口开始时间
     * @return 互动次数
     */
    int countByUserAndTopic(@Param("userId") Long userId, @Param("topicId") Long topicId,
                            @Param("startTime") LocalDateTime startTime);

    /**
     * 按话题聚合互动事件（用于热度计算）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 聚合结果列表：topic_id, interaction_type, total_count
     */
    List<Map<String, Object>> aggregateByTopic(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    /**
     * 按话题聚合全部互动事件（用于全量热度重算）
     *
     * @return 聚合结果列表：topic_id, interaction_type, total_count
     */
    List<Map<String, Object>> aggregateAllByTopic();

    List<Map<String, Object>> aggregateWeightedScoreAllByTopic(@Param("likeWeight") int likeWeight,
                                                               @Param("bookmarkWeight") int bookmarkWeight,
                                                               @Param("shareWeight") int shareWeight,
                                                               @Param("commentWeight") int commentWeight);

    /**
     * 按话题聚合指定时间段内的加权互动分（用于飙升榜）
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 聚合结果列表：topic_id, weighted_score, total_count
     */
    List<Map<String, Object>> aggregateWeightedScoreByTopic(@Param("startTime") LocalDateTime startTime,
                                                            @Param("endTime") LocalDateTime endTime,
                                                            @Param("likeWeight") int likeWeight,
                                                            @Param("bookmarkWeight") int bookmarkWeight,
                                                            @Param("shareWeight") int shareWeight,
                                                            @Param("commentWeight") int commentWeight);

    List<Map<String, Object>> aggregateByType(@Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 查询话题在指定时间段内的互动总量
     * @param topicId 话题ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 互动次数
     */
    int countByTopicAndTimeRange(@Param("topicId") Long topicId,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 按设备指纹聚合互动（用于设备指纹检测）
     * @param deviceFingerprint 设备指纹
     * @param startTime 开始时间
     * @return 不同用户数量
     */
    int countDistinctUserByDevice(@Param("deviceFingerprint") String deviceFingerprint,
                                  @Param("startTime") LocalDateTime startTime);

    int countByUserAndDevice(@Param("userId") Long userId,
                             @Param("deviceFingerprint") String deviceFingerprint,
                             @Param("startTime") LocalDateTime startTime);
}
