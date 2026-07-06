package com.jike.hotrank.engine.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户行为实体类
 * 对应数据库表：user_behavior
 */
@Data
public class UserBehavior {

    /** 行为ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 话题ID */
    private Long topicId;

    /** 互动类型 */
    private Integer interactionType;

    /** 设备指纹 */
    private String deviceFingerprint;

    /** IP地址 */
    private String ipAddress;

    /** 是否有效：0-无效(被拦截) 1-有效 */
    private Integer isValid;

    /** 无效原因 */
    private String invalidReason;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
