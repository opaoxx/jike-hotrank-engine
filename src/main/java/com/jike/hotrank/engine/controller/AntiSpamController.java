package com.jike.hotrank.engine.controller;

import com.jike.hotrank.engine.dto.ApiResponse;
import com.jike.hotrank.engine.service.AntiSpamReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 防刷检测控制器
 * 提供防刷检测报告接口
 *
 * @author JikeHotRank Team
 */
@Slf4j
@RestController
@RequestMapping("/api/anti-spam")
@RequiredArgsConstructor
public class AntiSpamController {

    private final AntiSpamReportService antiSpamReportService;

    /**
     * 获取防刷检测报告
     * <p>
     * 统计当日拦截的异常互动数量、涉及话题、疑似机器账号列表
     *
     * @return 防刷检测报告
     */
    @GetMapping("/report")
    public ApiResponse<Map<String, Object>> getReport() {
        log.info("查询防刷检测报告");
        Map<String, Object> report = antiSpamReportService.generateReport();
        return ApiResponse.success(report);
    }
}
