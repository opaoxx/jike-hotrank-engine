package com.jike.hotrank.engine.mapper;

import com.jike.hotrank.engine.entity.TopicScoreSnapshot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 话题热度快照Mapper接口
 */
@Mapper
public interface TopicScoreSnapshotMapper {

    /**
     * 插入快照记录
     */
    int insert(TopicScoreSnapshot snapshot);

    /**
     * 批量插入快照记录
     */
    int batchInsert(@Param("list") List<TopicScoreSnapshot> snapshots);

    /**
     * 查询指定时间点的榜单快照
     * @param snapshotTime 快照时间
     * @return 快照列表（按排名升序）
     */
    List<TopicScoreSnapshot> selectBySnapshotTime(@Param("snapshotTime") LocalDateTime snapshotTime);

    /**
     * 查询指定圈子在指定时间点的榜单快照
     * @param circleId 圈子ID
     * @param snapshotTime 快照时间
     * @return 快照列表（按排名升序）
     */
    List<TopicScoreSnapshot> selectByCircleAndTime(@Param("circleId") Long circleId,
                                                   @Param("snapshotTime") LocalDateTime snapshotTime);

    /**
     * 查询话题的历史热度趋势
     * @param topicId 话题ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 快照列表（按时间升序）
     */
    List<TopicScoreSnapshot> selectTopicTrend(@Param("topicId") Long topicId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);
}
