package com.example.analytics.util;

import com.example.analytics.model.OperationalEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class EventFactory {
    private static final List<String> CATEGORIES = List.of("Electronics", "Grocery", "Apparel", "Home", "Sports");
    private static final List<String> REGIONS = List.of("US-East", "US-West", "EU", "APAC");
    private static final List<String> STATUSES = List.of("SUCCESS", "SUCCESS", "SUCCESS", "PENDING", "FAILED");

    public static OperationalEvent random() {
        var r = ThreadLocalRandom.current();
        return new OperationalEvent(
                UUID.randomUUID().toString(),
                CATEGORIES.get(r.nextInt(CATEGORIES.size())),
                REGIONS.get(r.nextInt(REGIONS.size())),
                Math.round(r.nextDouble(5, 800) * 100.0) / 100.0,
                STATUSES.get(r.nextInt(STATUSES.size())),
                Instant.now()
        );
    }
}
