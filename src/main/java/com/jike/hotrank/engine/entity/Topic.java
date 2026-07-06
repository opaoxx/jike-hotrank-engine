package com.jike.hotrank.engine.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 话题实体类
 * 对应数据库表：topic
 */
@Data
public class Topic {

    /** 话题ID */
    private Long id;

    /** 所属圈子ID */
    private Long circleId;

    /** 话题标题 */
    private String title;

    /** 话题内容 */
    private String content;

    /** 作者用户ID */
    private Long authorId;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 当前热度分 */
    private BigDecimal currentScore;

    /** 总互动次数 */
    private Integer interactionCount;

    /** 状态：0-屏蔽 1-正常 2-待审核 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
