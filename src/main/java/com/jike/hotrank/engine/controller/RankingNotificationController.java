package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.service.RankingNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class RankingNotificationController {

    private final RankingNotificationService rankingNotificationService;

    @GetMapping(value = "/rankings/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamRankingEvents() {
        return rankingNotificationService.subscribe();
    }
}
