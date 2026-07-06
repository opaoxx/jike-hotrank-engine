package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.entity.UserBehavior;
import com.jike.hotrank.engine.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 防刷检测报告服务
 * 生成防刷检测统计数据
 *
 * @author JikeHotRank Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AntiSpamReportService {

    private final UserBehaviorMapper userBehaviorMapper;

    /**
     * 生成防刷检测报告
     *
     * @return 报告数据
     */
    public Map<String, Object> generateReport() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 查询当日所有异常行为
        List<UserBehavior> invalidBehaviors = userBehaviorMapper.selectInvalidBehaviors(todayStart, now);

        // 统计数据
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportTime", now.toString());
        report.put("periodStart", todayStart.toString());
        report.put("periodEnd", now.toString());

        // 1. 总拦截数量
        report.put("totalBlockedCount", invalidBehaviors.size());

        // 2. 按拦截原因分类统计
        Map<String, Long> reasonStats = invalidBehaviors.stream()
            .collect(Collectors.groupingBy(
                b -> b.getInvalidReason() != null ? b.getInvalidReason() : "未知原因",
                Collectors.counting()
            ));
        report.put("blockReasonStats", reasonStats);

        // 3. 涉及的话题ID列表
        Set<Long> affectedTopicIds = invalidBehaviors.stream()
            .map(UserBehavior::getTopicId)
            .collect(Collectors.toSet());
        report.put("affectedTopicCount", affectedTopicIds.size());
        report.put("affectedTopicIds", affectedTopicIds);

        // 4. 疑似机器账号列表（被拦截次数 >= 3 的用户）
        Map<Long, Long> userBlockCount = invalidBehaviors.stream()
            .collect(Collectors.groupingBy(UserBehavior::getUserId, Collectors.counting()));

        List<Map<String, Object>> suspiciousUsers = userBlockCount.entrySet().stream()
            .filter(entry -> entry.getValue() >= 3)
            .map(entry -> {
                Map<String, Object> user = new LinkedHashMap<>();
                user.put("userId", entry.getKey());
                user.put("blockCount", entry.getValue());
                return user;
            })
            .sorted((a, b) -> Long.compare((Long) b.get("blockCount"), (Long) a.get("blockCount")))
            .collect(Collectors.toList());

        report.put("suspiciousUserCount", suspiciousUsers.size());
        report.put("suspiciousUsers", suspiciousUsers);

        // 5. 按互动类型统计
        Map<String, Long> interactionTypeStats = invalidBehaviors.stream()
            .collect(Collectors.groupingBy(
                this::getInteractionTypeName,
                Collectors.counting()
            ));
        report.put("interactionTypeStats", interactionTypeStats);

        log.info("防刷检测报告生成完成：总拦截数={}, 涉及话题数={}, 疑似机器账号数={}",
                 invalidBehaviors.size(), affectedTopicIds.size(), suspiciousUsers.size());

        return report;
    }

    /**
     * 获取互动类型名称
     */
    private String getInteractionTypeName(UserBehavior behavior) {
        return switch (behavior.getInteractionType()) {
            case 1 -> "点赞";
            case 2 -> "收藏";
            case 3 -> "转发";
            case 5 -> "评论";
            default -> "未知";
        };
    }
}
