package com.tokenhunter.controller;

import com.tokenhunter.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return dashboardService.getTodaySummary();
    }

    /**
     * GET /api/dashboard/trend?range=7d|30d
     * range 默认 7d，非 "7d"/"30d" 的值一律回退到 7d。
     */
    @GetMapping("/trend")
    public List<Map<String, Object>> trend(@RequestParam(value = "range", defaultValue = "7d") String range) {
        int days = 7;
        if ("30d".equals(range)) {
            days = 30;
        }
        return dashboardService.getDailyTrend(days);
    }
}
