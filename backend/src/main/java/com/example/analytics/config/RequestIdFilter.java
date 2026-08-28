package com.example.analytics.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns a short correlation ID to every incoming request and puts it in
 * SLF4J's MDC, so every log line emitted while handling that request can be
 * grepped together — e.g. `grep "req=a3f9c1"` pulls the full story of one
 * request across controllers, services, and the Gemini client, even when
 * many requests are being handled concurrently.
 */
@Component
@Order(1) // run before RateLimitFilter so rejected requests are still traceable
public class RequestIdFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-Id", requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }
}
