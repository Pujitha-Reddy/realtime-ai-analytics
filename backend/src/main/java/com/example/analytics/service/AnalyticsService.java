package com.example.analytics.service;

import com.example.analytics.dto.MetricsSnapshot;
import com.example.analytics.model.OperationalEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final String TOTAL_EVENTS = "metrics:total_events";
    private static final String TOTAL_REVENUE = "metrics:total_revenue";
    private static final String SUCCESS_COUNT = "metrics:success_count";
    private static final String CATEGORY_COUNTS = "metrics:category_counts";
    private static final String REGION_REVENUE = "metrics:region_revenue";
    private static final String RECENT_EVENTS = "metrics:recent_events";
    private static final int RECENT_EVENTS_LIMIT = 100;

    private final RedisTemplate<String, String> redis;

    public AnalyticsService(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public void ingest(OperationalEvent event) {
        redis.opsForValue().increment(TOTAL_EVENTS);
        redis.opsForValue().increment(TOTAL_REVENUE, event.amount());
        if ("SUCCESS".equals(event.status())) {
            redis.opsForValue().increment(SUCCESS_COUNT);
        }
        redis.opsForHash().increment(CATEGORY_COUNTS, event.category(), 1);
        redis.opsForZSet().incrementScore(REGION_REVENUE, event.region(), event.amount());

        redis.opsForList().leftPush(RECENT_EVENTS, event.toSummary());
        redis.opsForList().trim(RECENT_EVENTS, 0, RECENT_EVENTS_LIMIT - 1);
    }

    public MetricsSnapshot getSnapshot() {
        long totalEvents = parseLong(redis.opsForValue().get(TOTAL_EVENTS));
        double totalRevenue = parseDouble(redis.opsForValue().get(TOTAL_REVENUE));
        long successCount = parseLong(redis.opsForValue().get(SUCCESS_COUNT));

        double avgOrderValue = totalEvents == 0 ? 0 : totalRevenue / totalEvents;
        double successRate = totalEvents == 0 ? 0 : (successCount * 100.0) / totalEvents;

        Map<String, Long> countByCategory = new LinkedHashMap<>();
        redis.opsForHash().entries(CATEGORY_COUNTS).forEach((k, v) ->
                countByCategory.put(k.toString(), Long.parseLong(v.toString())));

        Map<String, Double> revenueByRegion = new LinkedHashMap<>();
        var regionSet = redis.opsForZSet().rangeWithScores(REGION_REVENUE, 0, -1);
        if (regionSet != null) {
            regionSet.forEach(t -> revenueByRegion.put(t.getValue(), t.getScore()));
        }

        List<String> recent = redis.opsForList().range(RECENT_EVENTS, 0, 19);

        return new MetricsSnapshot(totalEvents, totalRevenue, avgOrderValue, successRate,
                countByCategory, revenueByRegion, recent == null ? List.of() : recent);
    }

    public List<String> getRecentEventSummaries(int limit) {
        List<String> events = redis.opsForList().range(RECENT_EVENTS, 0, limit - 1);
        return events == null ? List.of() : events;
    }

    private long parseLong(String s) { return s == null ? 0 : Long.parseLong(s); }
    private double parseDouble(String s) { return s == null ? 0 : Double.parseDouble(s); }
}
