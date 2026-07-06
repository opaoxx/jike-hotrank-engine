package com.jike.hotrank.engine.util;

public final class RankingLimits {

    public static final int GLOBAL_DEFAULT = 50;
    public static final int GLOBAL_MAX = 100;
    public static final int CIRCLE_DEFAULT = 20;
    public static final int CIRCLE_MAX = 100;
    public static final int NEWCOMER_DEFAULT = 10;
    public static final int NEWCOMER_MAX = 50;
    public static final int SURGING_DEFAULT = 10;
    public static final int SURGING_MAX = 50;
    public static final int PERSONALIZED_CANDIDATE_MULTIPLIER = 2;

    private RankingLimits() {
    }

    public static int global(Integer requested) {
        return normalize(requested, GLOBAL_DEFAULT, GLOBAL_MAX);
    }

    public static int circle(Integer requested) {
        return normalize(requested, CIRCLE_DEFAULT, CIRCLE_MAX);
    }

    public static int newcomer(Integer requested) {
        return normalize(requested, NEWCOMER_DEFAULT, NEWCOMER_MAX);
    }

    public static int surging(Integer requested) {
        return normalize(requested, SURGING_DEFAULT, SURGING_MAX);
    }

    public static int personalized(Integer requested) {
        return global(requested);
    }

    public static int personalizedCandidate(Integer requested) {
        return Math.multiplyExact(personalized(requested), PERSONALIZED_CANDIDATE_MULTIPLIER);
    }

    private static int normalize(Integer requested, int defaultLimit, int maxLimit) {
        if (requested == null || requested <= 0) {
            return defaultLimit;
        }
        return Math.min(requested, maxLimit);
    }
}
