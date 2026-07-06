package com.jike.hotrank.engine.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public class HeatScoreCalculator {

    private static final double DECAY_EXPONENT = 1.8;
    private static final double TIME_OFFSET = 2.0;

    public static final int WEIGHT_LIKE = 1;
    public static final int WEIGHT_BOOKMARK = 2;
    public static final int WEIGHT_SHARE = 3;
    public static final int WEIGHT_COMMENT = 5;

    private HeatScoreCalculator() {
    }

    public static BigDecimal calculateScore(long totalInteractionScore,
                                            LocalDateTime publishTime,
                                            LocalDateTime calculateTime) {
        return calculateScore(BigDecimal.valueOf(totalInteractionScore), publishTime, calculateTime);
    }

    public static BigDecimal calculateScore(BigDecimal totalInteractionScore,
                                            LocalDateTime publishTime,
                                            LocalDateTime calculateTime) {
        if (totalInteractionScore == null || totalInteractionScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        double ageHours = Duration.between(publishTime, calculateTime).toMillis() / (1000.0 * 60 * 60);
        if (ageHours < 0) {
            ageHours = 0;
        }

        double decayFactor = Math.pow(ageHours + TIME_OFFSET, DECAY_EXPONENT);
        double score = totalInteractionScore.doubleValue() / decayFactor;

        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    public static int getWeight(int interactionType) {
        return switch (interactionType) {
            case 1 -> WEIGHT_LIKE;
            case 2 -> WEIGHT_BOOKMARK;
            case 3 -> WEIGHT_SHARE;
            case 5 -> WEIGHT_COMMENT;
            default -> 0;
        };
    }

    public static long calculateTotalInteractionScore(int likeCount, int bookmarkCount,
                                                      int shareCount, int commentCount) {
        return (long) likeCount * WEIGHT_LIKE
             + (long) bookmarkCount * WEIGHT_BOOKMARK
             + (long) shareCount * WEIGHT_SHARE
             + (long) commentCount * WEIGHT_COMMENT;
    }

    public static BigDecimal calculateScore(long totalInteractionScore, LocalDateTime publishTime) {
        return calculateScore(totalInteractionScore, publishTime, LocalDateTime.now());
    }

    public static BigDecimal calculateScore(BigDecimal totalInteractionScore, LocalDateTime publishTime) {
        return calculateScore(totalInteractionScore, publishTime, LocalDateTime.now());
    }

    public static String getInteractionTypeName(int interactionType) {
        return switch (interactionType) {
            case 1 -> "like";
            case 2 -> "bookmark";
            case 3 -> "share";
            case 5 -> "comment";
            default -> "unknown";
        };
    }
}
