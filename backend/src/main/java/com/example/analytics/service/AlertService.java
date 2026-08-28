package com.example.analytics.service;

import com.example.analytics.dto.Alert;
import com.example.analytics.dto.MetricsSnapshot;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertService {

    // Active alerts keyed by rule id, so re-triggering the same rule updates
    // rather than duplicates, and resolving the condition clears it.
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public AlertService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void evaluate(MetricsSnapshot snapshot) {
        // Require a minimum sample size so alerts aren't noisy on startup
        if (snapshot.totalEvents() < 20) return;

        evaluateRule("low-success-rate",
                snapshot.successRate() < 70,
                "WARNING",
                String.format("Success rate has dropped to %.1f%% (below 70%% threshold)", snapshot.successRate()));

        long failedInSample = snapshot.recentEvents().stream()
                .filter(e -> e.contains("(FAILED)"))
                .count();
        double failedRatio = snapshot.recentEvents().isEmpty() ? 0 :
                (double) failedInSample / snapshot.recentEvents().size();

        evaluateRule("high-failure-spike",
                failedRatio > 0.4,
                "CRITICAL",
                String.format("%.0f%% of recent orders are failing — investigate immediately", failedRatio * 100));

        broadcast();
    }

    private void evaluateRule(String ruleId, boolean triggered, String severity, String message) {
        if (triggered) {
            activeAlerts.put(ruleId, new Alert(ruleId, severity, message, Instant.now()));
        } else {
            activeAlerts.remove(ruleId);
        }
    }

    private void broadcast() {
        messagingTemplate.convertAndSend("/topic/alerts", getActiveAlerts());
    }

    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }
}
