package com.jike.hotrank.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class RankingNotificationDTO {

    private String type;

    private LocalDateTime occurredAt;

    private Map<String, Object> payload;

    public static RankingNotificationDTO of(String type, Map<String, Object> payload) {
        RankingNotificationDTO notification = new RankingNotificationDTO();
        notification.setType(type);
        notification.setOccurredAt(LocalDateTime.now());
        notification.setPayload(payload);
        return notification;
    }
}
