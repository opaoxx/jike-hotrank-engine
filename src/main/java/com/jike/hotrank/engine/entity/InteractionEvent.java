package com.jike.hotrank.engine.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 互动事件实体类
 * 对应数据库表：interaction_event
 */
@Data
public class InteractionEvent {

    /** 事件ID */
    private Long id;

    /** 话题ID */
    private Long topicId;

    /** 用户ID */
    private Long userId;

    /** 互动类型：1-点赞 2-收藏 3-转发 5-评论 */
    private Integer interactionType;

    /** 设备指纹 */
    private String deviceFingerprint;

    /** IP地址 */
    private String ipAddress;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
