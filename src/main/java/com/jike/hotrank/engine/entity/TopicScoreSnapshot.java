package com.jike.hotrank.engine.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 话题热度快照实体类
 * 对应数据库表：topic_score_snapshot
 */
@Data
public class TopicScoreSnapshot {

    /** 快照ID */
    private Long id;

    /** 话题ID */
    private Long topicId;

    /** 圈子ID */
    private Long circleId;

    /** 热度分 */
    private BigDecimal score;

    /** 排名位置 */
    private Integer rankPosition;

    /** 快照时间 */
    private LocalDateTime snapshotTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
