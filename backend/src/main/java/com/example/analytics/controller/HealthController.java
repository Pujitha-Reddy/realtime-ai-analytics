package com.example.analytics.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final RedisTemplate<String, String> redis;

    public HealthController(RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        boolean redisHealthy = checkRedis();

        status.put("status", redisHealthy ? "ok" : "degraded");
        status.put("redis", redisHealthy ? "up" : "down");

        HttpStatus httpStatus = redisHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(status);
    }

    private boolean checkRedis() {
        try {
            redis.opsForValue().get("health:check");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
