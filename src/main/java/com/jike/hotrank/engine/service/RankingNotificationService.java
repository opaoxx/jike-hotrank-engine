package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.dto.RankingNotificationDTO;
import com.jike.hotrank.engine.entity.Topic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
public class RankingNotificationService {

    public static final String EVENT_RANKING_UPDATED = "ranking-updated";
    public static final String EVENT_TOP_N_ENTERED = "top-n-entered";

    private static final long STREAM_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(error -> emitters.remove(emitter));

        sendToEmitter(emitter, "connected", RankingNotificationDTO.of("connected",
            Map.of("subscriberCount", emitters.size())));
        return emitter;
    }

    public void publishRankingUpdated(int updatedTopicCount) {
        publish(EVENT_RANKING_UPDATED, RankingNotificationDTO.of(EVENT_RANKING_UPDATED,
            Map.of("updatedTopicCount", updatedTopicCount)));
    }

    public void publishTopNEntered(int threshold, Topic topic) {
        publish(EVENT_TOP_N_ENTERED, RankingNotificationDTO.of(EVENT_TOP_N_ENTERED,
            Map.of(
                "threshold", threshold,
                "topicId", topic.getId(),
                "title", topic.getTitle(),
                "score", topic.getCurrentScore()
            )));
    }

    public int subscriberCount() {
        return emitters.size();
    }

    private void publish(String eventName, RankingNotificationDTO notification) {
        for (SseEmitter emitter : emitters) {
            sendToEmitter(emitter, eventName, notification);
        }
    }

    private void sendToEmitter(SseEmitter emitter, String eventName, RankingNotificationDTO notification) {
        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(notification));
        } catch (IOException | IllegalStateException e) {
            emitters.remove(emitter);
            log.debug("Removed broken SSE subscriber", e);
        }
    }
}
