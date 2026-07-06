package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.entity.InteractionEvent;
import com.jike.hotrank.engine.service.InteractionWriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InteractionControllerTest {

    @Mock
    private InteractionWriteService interactionWriteService;

    @InjectMocks
    private InteractionController interactionController;

    @Test
    void shouldDelegateValidInteractionToWriteService() {
        InteractionEvent event = new InteractionEvent();
        event.setTopicId(1L);
        event.setUserId(99L);
        event.setInteractionType(1);

        when(interactionWriteService.recordInteraction(event))
            .thenReturn(InteractionWriteService.RecordResult.accepted(event));

        ApiResponse<InteractionEvent> response = interactionController.recordInteraction(event);

        assertEquals(0, response.getCode());
        assertEquals(event, response.getData());
        verify(interactionWriteService).recordInteraction(event);
    }

    @Test
    void shouldRejectUnsupportedInteractionTypeBeforeWriteService() {
        InteractionEvent event = new InteractionEvent();
        event.setTopicId(1L);
        event.setUserId(99L);
        event.setInteractionType(9);

        ApiResponse<InteractionEvent> response = interactionController.recordInteraction(event);

        assertEquals(400, response.getCode());
    }
}
