package com.example.analytics;

import com.example.analytics.dto.MetricsSnapshot;
import com.example.analytics.service.AnalyticsService;
import com.example.analytics.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Evaluation harness for the RAG pipeline: builds a FIXED, known snapshot of
 * metrics (mocked AnalyticsService, not the live pipeline) so we know exactly
 * what the "correct" answer should contain, then makes a REAL call to Gemini
 * and checks the response is actually grounded in that data.
 *
 * This is different from a unit test — it's not testing our own logic in
 * isolation, it's measuring whether the LLM correctly uses the context we
 * hand it. Skipped automatically if GEMINI_API_KEY isn't set (e.g. in CI
 * environments without secrets configured), so it never blocks a build.
 */
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class RagEvaluationTest {

    private RagService ragService;
    private AnalyticsService mockAnalyticsService;

    @BeforeEach
    void setUp() {
        mockAnalyticsService = mock(AnalyticsService.class);
        String apiKey = System.getenv("GEMINI_API_KEY");
        ragService = new RagService(mockAnalyticsService, apiKey, "gemini-3.6-flash", "gemini-embedding-001");
    }

    @Test
    void identifiesCorrectTopRevenueRegion() {
        // Fixed scenario: EU is unambiguously the top region by a wide margin.
        Map<String, Double> revenueByRegion = new LinkedHashMap<>();
        revenueByRegion.put("US-East", 1000.0);
        revenueByRegion.put("EU", 50000.0);
        revenueByRegion.put("APAC", 2000.0);
        revenueByRegion.put("US-West", 1500.0);

        MetricsSnapshot snapshot = new MetricsSnapshot(
                500, 54500.0, 109.0, 85.0,
                Map.of("Electronics", 200L, "Grocery", 300L),
                revenueByRegion,
                List.of("[t] Electronics order in EU worth $500.00 (SUCCESS)"));

        when(mockAnalyticsService.getSnapshot()).thenReturn(snapshot);
        when(mockAnalyticsService.getRecentEventSummaries(20)).thenReturn(
                List.of("[t] Electronics order in EU worth $500.00 (SUCCESS)"));

        String answer = ragService.answer("Which region is driving the most revenue right now?");

        assertTrue(answer.toUpperCase().contains("EU"),
                "Expected the answer to correctly identify EU as the top region. Got: " + answer);
    }

    @Test
    void reportsCorrectSuccessRate() {
        MetricsSnapshot snapshot = new MetricsSnapshot(
                1000, 100000.0, 100.0, 42.5,
                Map.of("Home", 400L),
                Map.of("APAC", 100000.0),
                List.of("[t] Home order in APAC worth $250.00 (FAILED)"));

        when(mockAnalyticsService.getSnapshot()).thenReturn(snapshot);
        when(mockAnalyticsService.getRecentEventSummaries(20)).thenReturn(
                List.of("[t] Home order in APAC worth $250.00 (FAILED)"));

        String answer = ragService.answer("What is the current success rate?");

        assertTrue(answer.contains("42.5"),
                "Expected the answer to cite the exact 42.5% success rate from context. Got: " + answer);
    }

    @Test
    void doesNotHallucinateWhenDataIsMinimal() {
        // Only one category exists in the data — the model should not invent others.
        MetricsSnapshot snapshot = new MetricsSnapshot(
                10, 1000.0, 100.0, 90.0,
                Map.of("Sports", 10L),
                Map.of("US-East", 1000.0),
                List.of("[t] Sports order in US-East worth $100.00 (SUCCESS)"));

        when(mockAnalyticsService.getSnapshot()).thenReturn(snapshot);
        when(mockAnalyticsService.getRecentEventSummaries(20)).thenReturn(
                List.of("[t] Sports order in US-East worth $100.00 (SUCCESS)"));

        String answer = ragService.answer("What categories have failed orders?");

        // "Sports" is the only category present and it has no FAILED status in
        // the sample — a well-grounded answer should not fabricate categories
        // like "Electronics" or "Grocery" that never appeared in the context.
        assertTrue(!answer.toLowerCase().contains("electronics") && !answer.toLowerCase().contains("grocery"),
                "Model should not hallucinate categories absent from the provided context. Got: " + answer);
    }
}
