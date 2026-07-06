package com.jike.hotrank.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RankingResponseDTO {

    private String rankingType;

    private Long circleId;

    private String circleName;

    private Long userId;

    private String updateTime;

    private List<RankingItemDTO> items;

    public static RankingResponseDTO ofGlobal(List<RankingItemDTO> items) {
        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankingType("global");
        response.setUpdateTime(LocalDateTime.now().toString());
        response.setItems(items);
        return response;
    }

    public static RankingResponseDTO ofPersonalized(Long userId, List<RankingItemDTO> items) {
        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankingType("personalized");
        response.setUserId(userId);
        response.setUpdateTime(LocalDateTime.now().toString());
        response.setItems(items);
        return response;
    }

    public static RankingResponseDTO ofCircle(Long circleId, String circleName, List<RankingItemDTO> items) {
        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankingType("circle");
        response.setCircleId(circleId);
        response.setCircleName(circleName);
        response.setUpdateTime(LocalDateTime.now().toString());
        response.setItems(items);
        return response;
    }

    public static RankingResponseDTO ofNewcomer(List<RankingItemDTO> items) {
        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankingType("newcomer");
        response.setUpdateTime(LocalDateTime.now().toString());
        response.setItems(items);
        return response;
    }

    public static RankingResponseDTO ofSurging(List<RankingItemDTO> items) {
        RankingResponseDTO response = new RankingResponseDTO();
        response.setRankingType("surging");
        response.setUpdateTime(LocalDateTime.now().toString());
        response.setItems(items);
        return response;
    }
}
