package com.example.analytics;

import com.example.analytics.dto.MetricsSnapshot;
import com.example.analytics.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Pure unit test — no Redis, no Spring context, no Docker. Verifies the
 * alert threshold rules in isolation so this runs in milliseconds as part
 * of every build, independent of the slower Testcontainers integration test.
 */
class AlertServiceTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final AlertService alertService = new AlertService(messagingTemplate);

    @Test
    void noAlertBelowMinimumSampleSize() {
        MetricsSnapshot snapshot = new MetricsSnapshot(
                5, 500.0, 100.0, 20.0, Map.of(), Map.of(), List.of());

        alertService.evaluate(snapshot);

        assertTrue(alertService.getActiveAlerts().isEmpty(),
                "Should not alert until there's a minimum sample size, to avoid noise on startup");
    }

    @Test
    void triggersWarningWhenSuccessRateDropsBelowThreshold() {
        MetricsSnapshot snapshot = new MetricsSnapshot(
                100, 10000.0, 100.0, 65.0, Map.of(), Map.of(), List.of());

        alertService.evaluate(snapshot);

        assertEquals(1, alertService.getActiveAlerts().size());
        assertEquals("WARNING", alertService.getActiveAlerts().get(0).severity());
    }

    @Test
    void clearsAlertWhenConditionResolves() {
        MetricsSnapshot bad = new MetricsSnapshot(100, 10000.0, 100.0, 50.0, Map.of(), Map.of(), List.of());
        MetricsSnapshot good = new MetricsSnapshot(100, 10000.0, 100.0, 95.0, Map.of(), Map.of(), List.of());

        alertService.evaluate(bad);
        assertEquals(1, alertService.getActiveAlerts().size());

        alertService.evaluate(good);
        assertTrue(alertService.getActiveAlerts().isEmpty(),
                "Alert should clear once success rate recovers above threshold");
    }
}
