package com.jike.hotrank.engine.task;

import com.jike.hotrank.engine.entity.TopicScoreSnapshot;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.mapper.TopicScoreSnapshotMapper;
import com.jike.hotrank.engine.service.TopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 热度快照定时任务
 * <p>
 * 每小时执行一次，将当前榜单TOP100存入快照表，支持历史榜单回溯
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotTask {

    private final TopicScoreSnapshotMapper topicScoreSnapshotMapper;
    private final TopicService topicService;

    /** 快照保存数量 */
    private static final int SNAPSHOT_LIMIT = 100;

    /**
     * 热度快照任务
     * <p>
     * 执行频率：每小时整点执行（cron: 秒 分 时 日 月 周）
     * <p>
     * 执行逻辑：
     * 1. 查询当前全站热榜TOP100
     * 2. 生成快照记录
     * 3. 批量写入快照表
     */
    @Scheduled(cron = "0 0 * * * *")  // 每小时整点执行
    public void takeSnapshot() {
        LocalDateTime snapshotTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        log.info("开始执行热度快照任务：snapshotTime={}", snapshotTime);

        try {
            // 1. 查询当前全站热榜TOP100
            List<Topic> topTopics = topicService.getGlobalHotRank(SNAPSHOT_LIMIT);

            if (topTopics == null || topTopics.isEmpty()) {
                log.info("热度快照任务完成：无话题数据");
                return;
            }

            // 2. 生成快照记录
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

            // 3. 批量写入快照表
            topicScoreSnapshotMapper.batchInsert(snapshots);
            log.info("热度快照任务完成：保存快照数量={}", snapshots.size());

        } catch (Exception e) {
            log.error("热度快照任务执行失败", e);
        }
    }
}
