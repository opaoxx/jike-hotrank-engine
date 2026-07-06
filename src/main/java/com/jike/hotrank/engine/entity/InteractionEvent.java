package com.jike.hotrank.engine.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Interaction event entity mapped to interaction_event.
 */
@Data
public class InteractionEvent {

    private Long id;

    private Long topicId;

    private Long userId;

    /**
     * 1-like, 2-bookmark, 3-share, 5-comment.
     */
    private Integer interactionType;

    private String deviceFingerprint;

    private String ipAddress;

    /**
     * Heat contribution multiplier after anti-spam checks.
     */
    private BigDecimal weightMultiplier;

    private LocalDateTime createdAt;
}
