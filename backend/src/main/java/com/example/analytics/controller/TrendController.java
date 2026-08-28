package com.example.analytics.controller;

import com.example.analytics.dto.TrendPoint;
import com.example.analytics.service.TrendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TrendController {
    private final TrendService trendService;

    public TrendController(TrendService trendService) {
        this.trendService = trendService;
    }

    @GetMapping("/api/trends")
    public List<TrendPoint> getTrends(@RequestParam(defaultValue = "24") int hours) {
        int capped = Math.min(hours, 72); // cap so nobody accidentally requests thousands of buckets
        return trendService.getTrend(capped);
    }
}
