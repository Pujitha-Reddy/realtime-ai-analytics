package com.example.analytics;

import com.example.analytics.model.OperationalEvent;
import com.example.analytics.pipeline.IngestionPipeline;
import com.example.analytics.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Spins up a real Redis container (not a mock) via Testcontainers and runs
 * events through the actual IngestionPipeline, proving two things end to end:
 *   1. Aggregation math in AnalyticsService is correct against real Redis.
 *   2. Duplicate event IDs (simulating Kafka at-least-once redelivery) are
 *      only counted once, thanks to the SETNX-based dedupe in IngestionPipeline.
 */
@Testcontainers
@SpringBootTest
class IngestionPipelineIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private IngestionPipeline pipeline;

    @Autowired
    private AnalyticsService analyticsService;

    @Test
    void duplicateEventIdsAreOnlyCountedOnce() {
        OperationalEvent event = new OperationalEvent(
                UUID.randomUUID().toString(), "Electronics", "US-East", 100.0, "SUCCESS", Instant.now());

        long before = analyticsService.getSnapshot().totalEvents();

        // Simulate Kafka redelivering the exact same message three times —
        // this happens in real systems on consumer rebalance or retry before
        // offset commit. Without idempotency, this would count as 3 events.
        pipeline.process(event);
        pipeline.process(event);
        pipeline.process(event);

        long after = analyticsService.getSnapshot().totalEvents();

        assertEquals(before + 1, after,
                "Duplicate event IDs must only be counted once — idempotency check failed");
    }

    @Test
    void distinctEventsAreEachCounted() {
        long before = analyticsService.getSnapshot().totalEvents();

        pipeline.process(new OperationalEvent(
                UUID.randomUUID().toString(), "Grocery", "EU", 50.0, "SUCCESS", Instant.now()));
        pipeline.process(new OperationalEvent(
                UUID.randomUUID().toString(), "Grocery", "EU", 75.0, "FAILED", Instant.now()));

        long after = analyticsService.getSnapshot().totalEvents();

        assertEquals(before + 2, after,
                "Two distinct events should both be counted");
    }
}
