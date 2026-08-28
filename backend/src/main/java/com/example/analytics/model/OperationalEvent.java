package com.example.analytics.model;

import java.time.Instant;

public record OperationalEvent(
        String id,
        String category,
        String region,
        double amount,
        String status,
        Instant timestamp
) {
    public String toSummary() {
        return String.format("[%s] %s order in %s worth $%.2f (%s)",
                timestamp, category, region, amount, status);
    }
}
