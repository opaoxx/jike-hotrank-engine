package com.jike.hotrank.engine.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 热度分计算工具类
 * <p>
 * 核心算法：时间衰减模型（Hacker News算法变种）
 * 公式：score = 总互动分 / (发布小时数 + 2)^1.8
 * <p>
 * 互动权重配置：
 * - 评论：5分（最高价值互动）
 * - 转发：3分（传播价值）
 * - 收藏：2分（收藏价值）
 * - 点赞：1分（基础互动）
 * <p>
 * 设计说明：
 * 1. 时间衰减指数1.8：介于线性(1.0)和平方(2.0)之间，平衡时效性和长尾效应
 * 2. 偏移量+2：防止新发布话题因小时数过小导致分数过高
 * 3. 使用BigDecimal保证精度，避免浮点数计算误差
 *
 * @author JikeHotRank Team
 */
public class HeatScoreCalculator {

    /** 时间衰减指数 */
    private static final double DECAY_EXPONENT = 1.8;

    /** 时间偏移量（小时） */
    private static final double TIME_OFFSET = 2.0;

    /** 互动权重：点赞 */
    public static final int WEIGHT_LIKE = 1;

    /** 互动权重：收藏 */
    public static final int WEIGHT_BOOKMARK = 2;

    /** 互动权重：转发 */
    public static final int WEIGHT_SHARE = 3;

    /** 互动权重：评论 */
    public static final int WEIGHT_COMMENT = 5;

    /**
     * 计算热度分
     * <p>
     * 算法：score = totalInteractionScore / (ageHours + 2)^1.8
     *
     * @param totalInteractionScore 总互动分（各类型互动次数 * 对应权重 的累加和）
     * @param publishTime 发布时间
     * @param calculateTime 计算时间（通常为当前时间）
     * @return 热度分（保留4位小数）
     */
    public static BigDecimal calculateScore(long totalInteractionScore,
                                            LocalDateTime publishTime,
                                            LocalDateTime calculateTime) {
        if (totalInteractionScore <= 0) {
            return BigDecimal.ZERO;
        }

        // 计算发布小时数
        double ageHours = Duration.between(publishTime, calculateTime).toMillis() / (1000.0 * 60 * 60);

        // 防止负数小时数（发布时间晚于计算时间的异常情况）
        if (ageHours < 0) {
            ageHours = 0;
        }

        // 计算时间衰减因子：(ageHours + 2)^1.8
        double decayFactor = Math.pow(ageHours + TIME_OFFSET, DECAY_EXPONENT);

        // 计算热度分
        double score = totalInteractionScore / decayFactor;

        // 转换为BigDecimal，保留4位小数
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 根据互动类型获取权重
     *
     * @param interactionType 互动类型：1-点赞 2-收藏 3-转发 5-评论
     * @return 权重值
     */
    public static int getWeight(int interactionType) {
        return switch (interactionType) {
            case 1 -> WEIGHT_LIKE;
            case 2 -> WEIGHT_BOOKMARK;
            case 3 -> WEIGHT_SHARE;
            case 5 -> WEIGHT_COMMENT;
            default -> 0;
        };
    }

    /**
     * 计算总互动分
     * <p>
     * 根据各类型互动次数和对应权重计算总分
     *
     * @param likeCount 点赞次数
     * @param bookmarkCount 收藏次数
     * @param shareCount 转发次数
     * @param commentCount 评论次数
     * @return 总互动分
     */
    public static long calculateTotalInteractionScore(int likeCount, int bookmarkCount,
                                                      int shareCount, int commentCount) {
        return (long) likeCount * WEIGHT_LIKE
             + (long) bookmarkCount * WEIGHT_BOOKMARK
             + (long) shareCount * WEIGHT_SHARE
             + (long) commentCount * WEIGHT_COMMENT;
    }

    /**
     * 计算热度分（简化版本，使用当前时间）
     *
     * @param totalInteractionScore 总互动分
     * @param publishTime 发布时间
     * @return 热度分
     */
    public static BigDecimal calculateScore(long totalInteractionScore, LocalDateTime publishTime) {
        return calculateScore(totalInteractionScore, publishTime, LocalDateTime.now());
    }

    /**
     * 获取互动类型名称（用于日志和调试）
     *
     * @param interactionType 互动类型代码
     * @return 互动类型名称
     */
    public static String getInteractionTypeName(int interactionType) {
        return switch (interactionType) {
            case 1 -> "点赞";
            case 2 -> "收藏";
            case 3 -> "转发";
            case 5 -> "评论";
            default -> "未知";
        };
    }
}
