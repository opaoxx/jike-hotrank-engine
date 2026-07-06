package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.config.HotRankProperties;
import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.task.HeatAggregationTask;
import com.jike.hotrank.engine.task.SnapshotTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationsControllerTest {

    @Mock
    private HeatAggregationTask heatAggregationTask;

    @Mock
    private SnapshotTask snapshotTask;

    private final HotRankProperties properties = new HotRankProperties();

    @Test
    void shouldRejectInvalidTokenWithoutTriggeringHeatAggregation() {
        OperationsController controller = new OperationsController(heatAggregationTask, snapshotTask, properties);

        ApiResponse<Map<String, Object>> response = controller.triggerHeatAggregation("wrong");

        assertEquals(403, response.getCode());
        verify(heatAggregationTask, never()).aggregateHeatWithLock();
    }

    @Test
    void shouldTriggerHeatAggregationWithValidToken() {
        OperationsController controller = new OperationsController(heatAggregationTask, snapshotTask, properties);

        ApiResponse<Map<String, Object>> response = controller.triggerHeatAggregation("ops_demo_token");

        assertEquals(0, response.getCode());
        assertEquals("heat-aggregation", response.getData().get("task"));
        verify(heatAggregationTask).aggregateHeatWithLock();
    }

    @Test
    void shouldTriggerSnapshotWithValidToken() {
        OperationsController controller = new OperationsController(heatAggregationTask, snapshotTask, properties);

        ApiResponse<Map<String, Object>> response = controller.triggerSnapshot("ops_demo_token");

        assertEquals(0, response.getCode());
        assertEquals("snapshot", response.getData().get("task"));
        verify(snapshotTask).takeSnapshotWithLock();
    }
}
