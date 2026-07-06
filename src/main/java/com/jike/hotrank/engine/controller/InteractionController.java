package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.entity.InteractionEvent;
import com.jike.hotrank.engine.service.InteractionWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionWriteService interactionWriteService;

    @PostMapping
    public ApiResponse<InteractionEvent> recordInteraction(@RequestBody InteractionEvent event) {
        log.info("Received interaction: topicId={}, userId={}, type={}",
            event.getTopicId(), event.getUserId(), event.getInteractionType());

        if (event.getTopicId() == null || event.getUserId() == null || event.getInteractionType() == null) {
            return ApiResponse.error(400, "参数不完整：topicId、userId、interactionType 不能为空");
        }

        if (!isSupportedInteractionType(event.getInteractionType())) {
            return ApiResponse.error(400, "互动类型无效：必须为 1(点赞)、2(收藏)、3(转发) 或 5(评论)");
        }

        InteractionWriteService.RecordResult result = interactionWriteService.recordInteraction(event);
        if (!result.accepted()) {
            return ApiResponse.error(result.code(), result.message());
        }
        return ApiResponse.success(result.event());
    }

    private boolean isSupportedInteractionType(Integer interactionType) {
        return interactionType == 1 || interactionType == 2 || interactionType == 3 || interactionType == 5;
    }
}
