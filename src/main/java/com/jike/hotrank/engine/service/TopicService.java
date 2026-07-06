package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.mapper.TopicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 话题服务类
 * 提供话题相关的业务操作
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicMapper topicMapper;

    /**
     * 根据ID查询话题
     *
     * @param id 话题ID
     * @return 话题信息
     */
    public Topic getById(Long id) {
        return topicMapper.selectById(id);
    }

    /**
     * 查询全站热榜
     *
     * @param limit 返回数量限制
     * @return 话题列表（按热度降序）
     */
    public List<Topic> getGlobalHotRank(Integer limit) {
        return topicMapper.selectGlobalHotRank(limit);
    }

    /**
     * 查询圈子热榜
     *
     * @param circleId 圈子ID
     * @param limit 返回数量限制
     * @return 话题列表（按热度降序）
     */
    public List<Topic> getCircleHotRank(Long circleId, Integer limit) {
        return topicMapper.selectCircleHotRank(circleId, limit);
    }

    /**
     * 查询新星榜（24小时内发布的热门话题）
     *
     * @param limit 返回数量限制
     * @return 话题列表
     */
    public List<Topic> getNewcomerRank(Integer limit) {
        return topicMapper.selectNewcomerRank(limit);
    }

    public List<Topic> listByStatus(Integer status) {
        return topicMapper.selectByStatus(status);
    }

    /**
     * 更新话题热度分
     *
     * @param topicId 话题ID
     * @param score 新的热度分
     * @param interactionCount 互动次数
     * @return 更新行数
     */
    public int updateScore(Long topicId, BigDecimal score, Integer interactionCount) {
        int rows = topicMapper.updateScore(topicId, score, interactionCount);
        log.debug("更新话题热度分：topicId={}, score={}, interactionCount={}", topicId, score, interactionCount);
        return rows;
    }

    /**
     * 批量更新话题热度分
     *
     * @param topicList 话题列表（包含id和新分数）
     * @return 更新行数
     */
    public int batchUpdateScore(List<Topic> topicList) {
        if (topicList == null || topicList.isEmpty()) {
            return 0;
        }
        int rows = topicMapper.batchUpdateScore(topicList);
        log.info("批量更新话题热度分：count={}", topicList.size());
        return rows;
    }

    /**
     * 创建话题
     *
     * @param topic 话题信息
     * @return 创建后的话题（包含生成的ID）
     */
    public Topic create(Topic topic) {
        topic.setCurrentScore(BigDecimal.ZERO);
        topic.setInteractionCount(0);
        topic.setStatus(1);
        topicMapper.insert(topic);
        log.info("创建话题成功：id={}, title={}", topic.getId(), topic.getTitle());
        return topic;
    }

    /**
     * 屏蔽话题
     *
     * @param topicId 话题ID
     * @return 更新行数
     */
    public int blockTopic(Long topicId) {
        int rows = topicMapper.updateStatus(topicId, 0);
        log.info("屏蔽话题成功：topicId={}, rows={}", topicId, rows);
        return rows;
    }

    /**
     * 恢复话题
     *
     * @param topicId 话题ID
     * @return 更新行数
     */
    public int unblockTopic(Long topicId) {
        int rows = topicMapper.updateStatus(topicId, 1);
        log.info("恢复话题成功：topicId={}, rows={}", topicId, rows);
        return rows;
    }

    /**
     * 标记话题为待审核
     *
     * @param topicId 话题ID
     * @return 更新行数
     */
    public int markForReview(Long topicId) {
        int rows = topicMapper.updateStatus(topicId, 2);
        log.info("标记话题待审核：topicId={}, rows={}", topicId, rows);
        return rows;
    }
}
