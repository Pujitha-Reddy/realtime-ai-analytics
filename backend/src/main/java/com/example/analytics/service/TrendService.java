package com.example.analytics.service;

import com.example.analytics.dto.TrendPoint;
import com.example.analytics.model.OperationalEvent;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrendService {

    private static final DateTimeFormatter BUCKET_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneOffset.UTC);

    private final RedisTemplate<String, String> redis;

    public TrendService(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    public void record(OperationalEvent event) {
        String bucket = BUCKET_FORMAT.format(event.timestamp());
        redis.opsForValue().increment("trend:events:" + bucket);
        redis.opsForValue().increment("trend:revenue:" + bucket, event.amount());
        // Bucket keys expire after 3 days so Redis doesn't grow unbounded
        redis.expire("trend:events:" + bucket, java.time.Duration.ofDays(3));
        redis.expire("trend:revenue:" + bucket, java.time.Duration.ofDays(3));
    }

    public List<TrendPoint> getTrend(int hours) {
        List<TrendPoint> points = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = hours - 1; i >= 0; i--) {
            Instant bucketTime = now.minus(i, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
            String bucket = BUCKET_FORMAT.format(bucketTime);
            String eventsStr = redis.opsForValue().get("trend:events:" + bucket);
            String revenueStr = redis.opsForValue().get("trend:revenue:" + bucket);
            points.add(new TrendPoint(
                    bucketTime.toString(),
                    eventsStr == null ? 0 : Long.parseLong(eventsStr),
                    revenueStr == null ? 0 : Double.parseDouble(revenueStr)
            ));
        }
        return points;
    }
}
