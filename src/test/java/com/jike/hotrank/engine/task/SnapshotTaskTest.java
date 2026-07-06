package com.jike.hotrank.engine.task;

import com.jike.hotrank.engine.mapper.TopicScoreSnapshotMapper;
import com.jike.hotrank.engine.service.TaskLockService;
import com.jike.hotrank.engine.service.TopicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SnapshotTaskTest {

    @Mock
    private TopicScoreSnapshotMapper topicScoreSnapshotMapper;

    @Mock
    private TopicService topicService;

    @Mock
    private TaskLockService taskLockService;

    @InjectMocks
    private SnapshotTask snapshotTask;

    @Test
    void shouldRunScheduledSnapshotThroughTaskLock() {
        snapshotTask.takeSnapshotWithLock();

        verify(taskLockService).runWithLock(eq("jike-hotrank:snapshot"), any(Runnable.class));
    }
}
