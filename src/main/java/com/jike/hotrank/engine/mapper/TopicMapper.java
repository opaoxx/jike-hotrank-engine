package com.jike.hotrank.engine.mapper;

import com.jike.hotrank.engine.entity.Topic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

/**
 * 话题Mapper接口
 *
 * @author JikeHotRank Team
 */
@Mapper
public interface TopicMapper {

    /**
     * 根据ID查询话题
     */
    Topic selectById(@Param("id") Long id);

    /**
     * 查询全站热榜TOP N
     * @param limit 返回数量
     * @return 话题列表（按热度降序）
     */
    List<Topic> selectGlobalHotRank(@Param("limit") Integer limit);

    /**
     * 查询圈子热榜TOP N
     * @param circleId 圈子ID
     * @param limit 返回数量
     * @return 话题列表（按热度降序）
     */
    List<Topic> selectCircleHotRank(@Param("circleId") Long circleId, @Param("limit") Integer limit);

    /**
     * 查询新星榜（24小时内发布的热门话题）
     * @param limit 返回数量
     * @return 话题列表
     */
    List<Topic> selectNewcomerRank(@Param("limit") Integer limit);

    /**
     * 更新话题热度分
     * @param topicId 话题ID
     * @param score 新的热度分
     * @param interactionCount 互动次数
     * @return 更新行数
     */
    int updateScore(@Param("topicId") Long topicId, @Param("score") BigDecimal score,
                    @Param("interactionCount") Integer interactionCount);

    /**
     * 批量更新话题热度分
     * @param topicList 话题列表（包含id和新分数）
     * @return 更新行数
     */
    int batchUpdateScore(@Param("list") List<Topic> topicList);

    /**
     * 插入话题
     */
    int insert(Topic topic);

    /**
     * 更新话题状态
     */
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * 查询指定状态的话题
     */
    List<Topic> selectByStatus(@Param("status") Integer status);

    /**
     * 查询所有话题
     */
    List<Topic> selectAll();
}
