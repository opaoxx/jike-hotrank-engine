package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.mapper.TaskLockMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskLockServiceTest {

    @Mock
    private TaskLockMapper taskLockMapper;

    @InjectMocks
    private TaskLockService taskLockService;

    @Test
    void shouldRunTaskAndReleaseLockWhenAcquired() {
        AtomicBoolean ran = new AtomicBoolean(false);
        when(taskLockMapper.getLock("task-a", 0)).thenReturn(1);
        when(taskLockMapper.releaseLock("task-a")).thenReturn(1);

        boolean executed = taskLockService.runWithLock("task-a", () -> ran.set(true));

        assertTrue(executed);
        assertTrue(ran.get());
        verify(taskLockMapper).releaseLock("task-a");
    }

    @Test
    void shouldSkipTaskWhenLockIsHeld() {
        AtomicBoolean ran = new AtomicBoolean(false);
        when(taskLockMapper.getLock("task-a", 0)).thenReturn(0);

        boolean executed = taskLockService.runWithLock("task-a", () -> ran.set(true));

        assertFalse(executed);
        assertFalse(ran.get());
        verify(taskLockMapper, never()).releaseLock("task-a");
    }
}
