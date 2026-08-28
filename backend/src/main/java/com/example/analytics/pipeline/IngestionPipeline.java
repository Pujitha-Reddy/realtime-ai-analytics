package com.example.analytics.pipeline;

import com.example.analytics.model.OperationalEvent;
import com.example.analytics.service.AlertService;
import com.example.analytics.service.AnalyticsService;
import com.example.analytics.service.TrendService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class IngestionPipeline {

    private static final String DEDUPE_KEY_PREFIX = "processed:event:";
    private static final Duration DEDUPE_TTL = Duration.ofHours(24);

    private final AnalyticsService analyticsService;
    private final TrendService trendService;
    private final AlertService alertService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redis;

    public IngestionPipeline(AnalyticsService analyticsService, TrendService trendService,
                              AlertService alertService, SimpMessagingTemplate messagingTemplate,
                              RedisTemplate<String, String> redis) {
        this.analyticsService = analyticsService;
        this.trendService = trendService;
        this.alertService = alertService;
        this.messagingTemplate = messagingTemplate;
        this.redis = redis;
    }

    public void process(OperationalEvent event) {
        // Atomic "set if absent" (Redis SETNX under the hood): returns true only
        // for the first caller to claim this event ID. If Kafka redelivers the
        // same message (rebalance, retry before offset commit), every later
        // call sees false here and we skip re-processing — this is what makes
        // ingestion idempotent under at-least-once delivery semantics.
        Boolean firstTimeSeen = redis.opsForValue()
                .setIfAbsent(DEDUPE_KEY_PREFIX + event.id(), "1", DEDUPE_TTL);

        if (firstTimeSeen == null || !firstTimeSeen) {
            return; // duplicate delivery — already processed, skip silently
        }

        analyticsService.ingest(event);
        trendService.record(event);
        var snapshot = analyticsService.getSnapshot();
        alertService.evaluate(snapshot);
        messagingTemplate.convertAndSend("/topic/metrics", snapshot);
    }
}
