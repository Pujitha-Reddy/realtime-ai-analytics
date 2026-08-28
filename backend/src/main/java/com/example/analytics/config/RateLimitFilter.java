package com.example.analytics.config;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-client-IP token bucket rate limiter, applied only to the AI chat
 * endpoints. Each Gemini call costs real quota/latency, so an unthrottled
 * endpoint is both a cost risk and a denial-of-service vector once deployed
 * publicly. 10 requests/minute is generous for a human chatting, but blocks
 * a script hammering the endpoint.
 */
@Component
public class RateLimitFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final int REQUESTS_PER_MINUTE = 10;

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!request.getRequestURI().startsWith("/api/chat")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client {} on {}", clientIp, request.getRequestURI());
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please wait before asking again.\"}");
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(REQUESTS_PER_MINUTE)
                        .refillGreedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1)))
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // X-Forwarded-For is set by proxies/load balancers (e.g. Render) in front
        // of the app; fall back to the direct remote address for local dev.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
