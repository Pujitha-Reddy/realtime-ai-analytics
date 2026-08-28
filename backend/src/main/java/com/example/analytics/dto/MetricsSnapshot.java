package com.example.analytics.dto;

import java.util.List;
import java.util.Map;

public record MetricsSnapshot(
        long totalEvents,
        double totalRevenue,
        double avgOrderValue,
        double successRate,
        Map<String, Long> countByCategory,
        Map<String, Double> revenueByRegion,
        List<String> recentEvents
) {}
