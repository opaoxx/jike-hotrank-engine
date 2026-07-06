package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.mapper.TaskLockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskLockService {

    private final TaskLockMapper taskLockMapper;

    @Transactional
    public boolean runWithLock(String lockName, Runnable task) {
        Integer acquired = taskLockMapper.getLock(lockName, 0);
        if (!Integer.valueOf(1).equals(acquired)) {
            log.info("Skip scheduled task because lock is held: lockName={}", lockName);
            return false;
        }

        try {
            task.run();
            return true;
        } finally {
            Integer released = taskLockMapper.releaseLock(lockName);
            if (!Integer.valueOf(1).equals(released)) {
                log.warn("Scheduled task lock release returned unexpected value: lockName={}, released={}",
                    lockName, released);
            }
        }
    }
}
