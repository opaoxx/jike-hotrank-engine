package com.jike.hotrank.engine.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RankingNotificationServiceTest {

    @Test
    void shouldRegisterSseSubscriber() {
        RankingNotificationService service = new RankingNotificationService();

        SseEmitter emitter = service.subscribe();

        assertNotNull(emitter);
        assertEquals(1, service.subscriberCount());
    }
}
