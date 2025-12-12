package com.tailai.hr.controller;

import com.tailai.common.result.Result;
import com.tailai.hr.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据统计控制器
 *
 * @author Tailai
 */
@Tag(name = "数据统计", description = "人事数据统计分析接口")
@RestController
@RequestMapping("/api/hr/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "数据概览", description = "获取系统数据概览。需要Authorization请求头")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard(
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return statisticsService.getDashboard();
    }

    @Operation(summary = "入离职趋势", description = "最近12个月的入离职趋势统计。需要Authorization请求头")
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend(
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return statisticsService.getTrend();
    }

    @Operation(summary = "部门分布", description = "各部门人员分布统计。需要Authorization请求头")
    @GetMapping("/department")
    public Result<Map<String, Object>> getDepartmentDistribution(
            @Parameter(description = "JWT Token", required = true, example = "Bearer xxx") 
            @RequestHeader("Authorization") String token) {
        return statisticsService.getDepartmentDistribution();
    }
}

