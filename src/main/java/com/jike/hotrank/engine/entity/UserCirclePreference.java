package com.jike.hotrank.engine.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户圈子偏好实体类
 * 对应数据库表：user_circle_preference
 */
@Data
public class UserCirclePreference {

    /** 偏好ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 圈子ID */
    private Long circleId;

    /** 偏好权重（0.01-10.00） */
    private BigDecimal weight;

    /** 互动次数 */
    private Integer interactionCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
