package com.jike.hotrank.engine.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 榜单排名项DTO
 */
@Data
public class RankingItemDTO {

    /** 排名位置 */
    private Integer rank;

    /** 话题ID */
    private Long topicId;

    /** 话题标题 */
    private String title;

    /** 圈子ID */
    private Long circleId;

    /** 圈子名称 */
    private String circleName;

    /** 热度分 */
    private BigDecimal score;

    /** 互动次数 */
    private Integer interactionCount;

    /** 作者ID */
    private Long authorId;

    /** 发布时间（ISO格式） */
    private String publishTime;
}
