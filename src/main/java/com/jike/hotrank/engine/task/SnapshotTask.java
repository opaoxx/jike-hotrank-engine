package com.jike.hotrank.engine.task;

import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.entity.TopicScoreSnapshot;
import com.jike.hotrank.engine.mapper.TopicScoreSnapshotMapper;
import com.jike.hotrank.engine.service.TaskLockService;
import com.jike.hotrank.engine.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotTask {

    private static final int SNAPSHOT_LIMIT = 100;

    private final TopicScoreSnapshotMapper topicScoreSnapshotMapper;
    private final TopicService topicService;
    private final TaskLockService taskLockService;

    @Scheduled(cron = "0 0 * * * *")
    public void takeSnapshotWithLock() {
        taskLockService.runWithLock("jike-hotrank:snapshot", this::takeSnapshot);
    }

    public void takeSnapshot() {
        LocalDateTime snapshotTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        log.info("Start ranking snapshot: snapshotTime={}", snapshotTime);

        try {
            List<Topic> topTopics = topicService.getGlobalHotRank(SNAPSHOT_LIMIT);
            if (topTopics == null || topTopics.isEmpty()) {
                log.info("Ranking snapshot finished: no topic data");
                return;
            }

            List<TopicScoreSnapshot> snapshots = new ArrayList<>();
            for (int i = 0; i < topTopics.size(); i++) {
                Topic topic = topTopics.get(i);
                TopicScoreSnapshot snapshot = new TopicScoreSnapshot();
                snapshot.setTopicId(topic.getId());
                snapshot.setCircleId(topic.getCircleId());
                snapshot.setScore(topic.getCurrentScore());
                snapshot.setRankPosition(i + 1);
                snapshot.setSnapshotTime(snapshotTime);
                snapshots.add(snapshot);
            }

            topicScoreSnapshotMapper.batchInsert(snapshots);
            log.info("Ranking snapshot finished: savedSnapshots={}", snapshots.size());
        } catch (Exception e) {
            log.error("Ranking snapshot failed", e);
        }
    }
}
