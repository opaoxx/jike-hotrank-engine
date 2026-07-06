package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @GetMapping("/heat-distribution")
    public ApiResponse<Map<String, Object>> heatDistribution() {
        return ApiResponse.success(analysisService.heatDistribution());
    }

    @GetMapping("/interaction-stats")
    public ApiResponse<Map<String, Object>> interactionStats(@RequestParam(required = false) Integer hours) {
        return ApiResponse.success(analysisService.interactionStats(hours));
    }

    @GetMapping("/circle-activity")
    public ApiResponse<Map<String, Object>> circleActivity() {
        return ApiResponse.success(analysisService.circleActivity());
    }

    @GetMapping("/anti-cheat-stats")
    public ApiResponse<Map<String, Object>> antiCheatStats(@RequestParam(required = false) Integer days) {
        return ApiResponse.success(analysisService.antiCheatStats(days));
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(analysisService.overview());
    }
}
