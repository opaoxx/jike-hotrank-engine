package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.config.HotRankProperties;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.task.HeatAggregationTask;
import com.jike.hotrank.engine.task.SnapshotTask;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OperationsController {

    private final HeatAggregationTask heatAggregationTask;
    private final SnapshotTask snapshotTask;
    private final HotRankProperties properties;

    @PostMapping("/heat-aggregation")
    public ApiResponse<Map<String, Object>> triggerHeatAggregation(@RequestParam String token) {
        if (!isValidToken(token)) {
            return ApiResponse.error(403, "运维 token 无效");
        }

        heatAggregationTask.aggregateHeatWithLock();
        return ApiResponse.success(triggerResult("heat-aggregation"));
    }

    @PostMapping("/snapshot")
    public ApiResponse<Map<String, Object>> triggerSnapshot(@RequestParam String token) {
        if (!isValidToken(token)) {
            return ApiResponse.error(403, "运维 token 无效");
        }

        snapshotTask.takeSnapshotWithLock();
        return ApiResponse.success(triggerResult("snapshot"));
    }

    private boolean isValidToken(String token) {
        return properties.getOperations().getToken().equals(token);
    }

    private Map<String, Object> triggerResult(String taskName) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("task", taskName);
        result.put("triggeredAt", LocalDateTime.now().toString());
        result.put("status", "submitted");
        return result;
    }
}
