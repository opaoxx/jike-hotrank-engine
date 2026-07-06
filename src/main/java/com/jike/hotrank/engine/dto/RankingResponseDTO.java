package com.jike.hotrank.engine.dto;

import lombok.Data;
import java.util.List;

/**
 * 榜单响应DTO
 */
@Data
public class RankingResponseDTO {

    /** 榜单类型：global-全站 hot-圈子热榜 newcomer-新星榜 surging-飙升榜 */
    private String rankingType;

    /** 圈子ID（仅圈子热榜时有值） */
    private Long circleId;

    /** 圈子名称（仅圈子热榜时有值） */
    private String circleName;

    /** 更新时间 */
    private String updateTime;

    /** 排名列表 */
    private List<RankingItemDTO> items;

    /**
     * 创建全站热榜响应
     */
    public static RankingResponseDTO ofGlobal(List<RankingItemDTO> items) {
        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankingType("global");
        response.setUpdateTime(java.time.LocalDateTime.now().toString());
        response.setItems(items);
        return response;
    }

    /**
     * 创建圈子热榜响应
     */
    public static RankingResponseDTO ofCircle(Long circleId, String circleName, List<RankingItemDTO> items) {
        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankingType("circle");
        response.setCircleId(circleId);
        response.setCircleName(circleName);
        response.setUpdateTime(java.time.LocalDateTime.now().toString());
        response.setItems(items);
        return response;
    }

    /**
     * 创建新星榜响应
     */
    public static RankingResponseDTO ofNewcomer(List<RankingItemDTO> items) {
        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankingType("newcomer");
        response.setUpdateTime(java.time.LocalDateTime.now().toString());
        response.setItems(items);
        return response;
    }

    /**
     * 创建飙升榜响应
     */
    public static RankingResponseDTO ofSurging(List<RankingItemDTO> items) {
        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankingType("surging");
        response.setUpdateTime(java.time.LocalDateTime.now().toString());
        response.setItems(items);
        return response;
    }
}
