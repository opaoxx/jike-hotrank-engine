package com.jike.hotrank.engine.service;

import com.jike.hotrank.engine.cache.RankingCacheManager;
import com.jike.hotrank.engine.entity.Circle;
import com.jike.hotrank.engine.entity.Topic;
import com.jike.hotrank.engine.entity.UserBehavior;
import com.jike.hotrank.engine.mapper.UserBehaviorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final List<ScoreRange> SCORE_RANGES = List.of(
        new ScoreRange("0~10", BigDecimal.ZERO, new BigDecimal("10")),
        new ScoreRange("10~50", new BigDecimal("10"), new BigDecimal("50")),
        new ScoreRange("50~100", new BigDecimal("50"), new BigDecimal("100")),
        new ScoreRange("100~500", new BigDecimal("100"), new BigDecimal("500")),
        new ScoreRange("500+", new BigDecimal("500"), null)
    );

    private final TopicService topicService;
    private final CircleService circleService;
    private final InteractionEventService interactionEventService;
    private final UserBehaviorMapper userBehaviorMapper;
    private final RankingCacheManager cacheManager;

    public Map<String, Object> heatDistribution() {
        List<Topic> topics = topicService.listByStatus(1);
        List<BigDecimal> scores = topics.stream()
            .map(Topic::getCurrentScore)
            .map(score -> score != null ? score : BigDecimal.ZERO)
            .sorted()
            .toList();

        List<Map<String, Object>> ranges = SCORE_RANGES.stream()
            .map(range -> toRangeStats(range, scores))
            .toList();

        Topic maxTopic = topics.stream()
            .max(Comparator.comparing(topic -> nonNullScore(topic.getCurrentScore())))
            .orElse(null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topicCount", topics.size());
        result.put("ranges", ranges);
        result.put("maxScore", scores.isEmpty() ? BigDecimal.ZERO : scores.get(scores.size() - 1));
        result.put("avgScore", average(scores));
        result.put("medianScore", median(scores));
        result.put("maxTopic", maxTopic == null ? null : Map.of(
            "topicId", maxTopic.getId(),
            "title", maxTopic.getTitle(),
            "score", nonNullScore(maxTopic.getCurrentScore())
        ));
        return result;
    }

    public Map<String, Object> interactionStats(Integer hours) {
        int actualHours = normalizeRange(hours, 24, 1, 168);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(actualHours);
        List<Map<String, Object>> rows = interactionEventService.aggregateByType(startTime, endTime);
        long total = rows.stream().mapToLong(row -> getLong(row, "total_count", "totalCount")).sum();

        List<Map<String, Object>> byType = rows.stream()
            .map(row -> {
                int type = getLong(row, "interaction_type", "interactionType").intValue();
                long count = getLong(row, "total_count", "totalCount");
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("type", type);
                item.put("name", interactionTypeName(type));
                item.put("count", count);
                item.put("percentage", percentage(count, total));
                return item;
            })
            .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hours", actualHours);
        result.put("periodStart", startTime.toString());
        result.put("periodEnd", endTime.toString());
        result.put("total", total);
        result.put("byType", byType);
        return result;
    }

    public Map<String, Object> circleActivity() {
        List<Circle> circles = circleService.listAllEnabled();
        Map<Long, Circle> circleMap = circles.stream()
            .collect(Collectors.toMap(Circle::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<Topic>> topicsByCircle = topicService.listByStatus(1).stream()
            .collect(Collectors.groupingBy(Topic::getCircleId));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Circle circle : circles) {
            List<Topic> topics = topicsByCircle.getOrDefault(circle.getId(), List.of());
            rows.add(toCircleActivity(circle, topics));
        }

        topicsByCircle.entrySet().stream()
            .filter(entry -> !circleMap.containsKey(entry.getKey()))
            .map(entry -> toCircleActivity(null, entry.getValue()))
            .forEach(rows::add);

        rows.sort((a, b) -> Long.compare((Long) b.get("interactionCount"), (Long) a.get("interactionCount")));
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).put("rank", i + 1);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("circleCount", rows.size());
        result.put("items", rows);
        return result;
    }

    public Map<String, Object> antiCheatStats(Integer days) {
        int actualDays = normalizeRange(days, 7, 1, 30);
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = LocalDate.now().minusDays(actualDays - 1L).atStartOfDay();
        List<UserBehavior> invalidBehaviors = userBehaviorMapper.selectInvalidBehaviors(startTime, endTime);

        Map<String, Long> byReason = invalidBehaviors.stream()
            .collect(Collectors.groupingBy(
                behavior -> behavior.getInvalidReason() != null ? behavior.getInvalidReason() : "unknown",
                LinkedHashMap::new,
                Collectors.counting()
            ));

        Map<String, Long> dailyTrend = invalidBehaviors.stream()
            .collect(Collectors.groupingBy(
                behavior -> behavior.getCreatedAt().toLocalDate().toString(),
                LinkedHashMap::new,
                Collectors.counting()
            ));

        List<Map<String, Object>> topTopics = invalidBehaviors.stream()
            .collect(Collectors.groupingBy(UserBehavior::getTopicId, Collectors.counting()))
            .entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(10)
            .map(entry -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("topicId", entry.getKey());
                item.put("blockedCount", entry.getValue());
                return item;
            })
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", actualDays);
        result.put("periodStart", startTime.toString());
        result.put("periodEnd", endTime.toString());
        result.put("totalBlockedCount", invalidBehaviors.size());
        result.put("byReason", byReason);
        result.put("dailyTrend", dailyTrend);
        result.put("topTopics", topTopics);
        return result;
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("heatDistribution", heatDistribution());
        result.put("interactionStats", interactionStats(24));
        result.put("circleActivity", circleActivity());
        result.put("antiCheatStats", antiCheatStats(7));
        result.put("cacheStats", cacheManager.getStats());
        return result;
    }

    private Map<String, Object> toRangeStats(ScoreRange range, List<BigDecimal> scores) {
        long count = scores.stream().filter(range::contains).count();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("range", range.label());
        item.put("count", count);
        item.put("percentage", percentage(count, scores.size()));
        return item;
    }

    private Map<String, Object> toCircleActivity(Circle circle, List<Topic> topics) {
        long interactionCount = topics.stream()
            .map(Topic::getInteractionCount)
            .mapToLong(count -> count != null ? count : 0)
            .sum();
        BigDecimal avgScore = average(topics.stream()
            .map(Topic::getCurrentScore)
            .map(this::nonNullScore)
            .toList());

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("circleId", circle != null ? circle.getId() : topics.getFirst().getCircleId());
        item.put("circleName", circle != null ? circle.getName() : "未知圈子");
        item.put("topicCount", topics.size());
        item.put("avgScore", avgScore);
        item.put("interactionCount", interactionCount);
        return item;
    }

    private int normalizeRange(Integer requested, int defaultValue, int min, int max) {
        if (requested == null) {
            return defaultValue;
        }
        return Math.max(min, Math.min(max, requested));
    }

    private BigDecimal nonNullScore(BigDecimal score) {
        return score != null ? score : BigDecimal.ZERO;
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<BigDecimal> sortedValues) {
        if (sortedValues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int middle = sortedValues.size() / 2;
        if (sortedValues.size() % 2 == 1) {
            return sortedValues.get(middle);
        }
        return sortedValues.get(middle - 1)
            .add(sortedValues.get(middle))
            .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private Long getLong(Map<String, Object> row, String snakeCaseKey, String camelCaseKey) {
        Object value = row.get(snakeCaseKey);
        if (value == null) {
            value = row.get(camelCaseKey);
        }
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Aggregation result missing numeric field: " + snakeCaseKey);
        }
        return number.longValue();
    }

    private String interactionTypeName(int interactionType) {
        return switch (interactionType) {
            case 1 -> "like";
            case 2 -> "bookmark";
            case 3 -> "share";
            case 5 -> "comment";
            default -> "unknown";
        };
    }

    private record ScoreRange(String label, BigDecimal minInclusive, BigDecimal maxExclusive) {
        boolean contains(BigDecimal score) {
            return score.compareTo(minInclusive) >= 0
                && (maxExclusive == null || score.compareTo(maxExclusive) < 0);
        }
    }
}
