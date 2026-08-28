package com.example.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final String RATE_LIMITED_MESSAGE =
            "The AI assistant is temporarily rate-limited by the upstream provider. Please wait a moment and try again.";

    private final AnalyticsService analyticsService;
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String chatModel;
    private final String embeddingModel;

    public RagService(AnalyticsService analyticsService,
                       @Value("${app.gemini.api-key}") String apiKey,
                       @Value("${app.gemini.chat-model}") String chatModel,
                       @Value("${app.gemini.embedding-model}") String embeddingModel) {
        this.analyticsService = analyticsService;
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("Content-Type", "application/json")
                .exchangeStrategies(strategies)
                .build();
    }

    public String answer(String question) {
        log.info("Handling chat question (length={})", question.length());
        try {
            RetrievalResult retrieval = retrieve(question);
            if (retrieval.noData()) {
                log.warn("No event data available yet — returning fallback message");
                return "No event data has streamed in yet — give it a few seconds and try again.";
            }
            return generateAnswer(question, retrieval.context());
        } catch (WebClientResponseException e) {
            if (isRateLimited(e)) {
                log.warn("Gemini rate limit hit — returning graceful fallback to caller");
                return RATE_LIMITED_MESSAGE;
            }
            throw e;
        }
    }

    public Flux<String> streamAnswer(String question) {
        log.info("Handling streaming chat question (length={})", question.length());
        RetrievalResult retrieval;
        try {
            retrieval = retrieve(question);
        } catch (WebClientResponseException e) {
            if (isRateLimited(e)) {
                log.warn("Gemini rate limit hit during retrieval — returning graceful fallback to caller");
                return Flux.just(RATE_LIMITED_MESSAGE);
            }
            throw e;
        }
        if (retrieval.noData()) {
            return Flux.just("No event data has streamed in yet — give it a few seconds and try again.");
        }
        return streamGenerateAnswer(question, retrieval.context());
    }

    private boolean isRateLimited(WebClientResponseException e) {
        return e.getStatusCode() == HttpStatusCode.valueOf(429);
    }

    private record RetrievalResult(String context, boolean noData) {}

    private RetrievalResult retrieve(String question) {
        List<String> recentEvents = analyticsService.getRecentEventSummaries(20);
        if (recentEvents.isEmpty()) {
            return new RetrievalResult(null, true);
        }

        List<String> inputs = new ArrayList<>();
        inputs.add(question);
        inputs.addAll(recentEvents);

        List<float[]> embeddings = embedBatch(inputs);
        float[] questionEmbedding = embeddings.get(0);
        List<float[]> eventEmbeddings = embeddings.subList(1, embeddings.size());

        List<String> topEvents = rankBySimilarity(recentEvents, eventEmbeddings, questionEmbedding, 5);
        log.debug("Retrieved {} relevant events out of {} candidates", topEvents.size(), recentEvents.size());
        return new RetrievalResult(buildContext(topEvents), false);
    }

    private List<float[]> embedBatch(List<String> inputs) {
        var body = mapper.createObjectNode();
        var requests = body.putArray("requests");
        for (String text : inputs) {
            var req = mapper.createObjectNode();
            req.put("model", "models/" + embeddingModel);
            var content = mapper.createObjectNode();
            var parts = content.putArray("parts");
            parts.addObject().put("text", text);
            req.set("content", content);
            req.put("outputDimensionality", 768);
            requests.add(req);
        }

        JsonNode response = webClient.post()
                .uri("/models/" + embeddingModel + ":batchEmbedContents?key=" + apiKey)
                .bodyValue(body.toString())
                .retrieve()
                .onStatus(status -> status.value() == 429, resp -> resp.createException())
                .bodyToMono(String.class)
                .map(this::parseJson)
                .doOnError(WebClientResponseException.class, e ->
                        log.error("Gemini embeddings API call failed: status={}, body={}",
                                e.getStatusCode(), e.getResponseBodyAsString()))
                .block();

        List<float[]> result = new ArrayList<>();
        for (JsonNode item : response.get("embeddings")) {
            JsonNode vec = item.get("values");
            float[] f = new float[vec.size()];
            for (int i = 0; i < vec.size(); i++) f[i] = (float) vec.get(i).asDouble();
            result.add(f);
        }
        return result;
    }

    private List<String> rankBySimilarity(List<String> events, List<float[]> eventEmbeddings,
                                           float[] queryEmbedding, int topK) {
        record Scored(String text, double score) {}
        List<Scored> scored = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            scored.add(new Scored(events.get(i), cosineSimilarity(queryEmbedding, eventEmbeddings.get(i))));
        }
        return scored.stream()
                .sorted(Comparator.comparingDouble(Scored::score).reversed())
                .limit(topK)
                .map(Scored::text)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-9);
    }

    String buildContext(List<String> topEvents) {
        var snapshot = analyticsService.getSnapshot();
        StringBuilder sb = new StringBuilder();
        sb.append("Current aggregate metrics:\n");
        sb.append("- Total events: ").append(snapshot.totalEvents()).append("\n");
        sb.append("- Total revenue: $").append(String.format("%.2f", snapshot.totalRevenue())).append("\n");
        sb.append("- Avg order value: $").append(String.format("%.2f", snapshot.avgOrderValue())).append("\n");
        sb.append("- Success rate: ").append(String.format("%.1f", snapshot.successRate())).append("%\n");

        var roundedRevenueByRegion = new LinkedHashMap<String, String>();
        snapshot.revenueByRegion().forEach((region, revenue) ->
                roundedRevenueByRegion.put(region, String.format("$%.2f", revenue)));
        sb.append("- Revenue by region: ").append(roundedRevenueByRegion).append("\n");

        sb.append("- Count by category: ").append(snapshot.countByCategory()).append("\n\n");
        sb.append("Most relevant recent events:\n");
        topEvents.forEach(e -> sb.append("- ").append(e).append("\n"));
        return sb.toString();
    }

    private String buildPrompt(String question, String context) {
        return "You are a real-time operational analytics assistant. Answer using ONLY the " +
                "context below. Be concise and cite specific numbers. Use only the formatted dollar " +
                "figures given — never include raw unrounded decimal values in parentheses or " +
                "anywhere else in your answer.\n\nContext:\n" + context +
                "\n\nQuestion: " + question;
    }

    private String generateAnswer(String question, String context) {
        var body = mapper.createObjectNode();
        var contents = body.putArray("contents");
        var content = contents.addObject();
        var parts = content.putArray("parts");
        parts.addObject().put("text", buildPrompt(question, context));

        JsonNode response = webClient.post()
                .uri("/models/" + chatModel + ":generateContent?key=" + apiKey)
                .bodyValue(body.toString())
                .retrieve()
                .onStatus(status -> status.value() == 429, resp -> resp.createException())
                .bodyToMono(String.class)
                .map(this::parseJson)
                .doOnError(WebClientResponseException.class, e ->
                        log.error("Gemini chat API call failed: status={}, body={}",
                                e.getStatusCode(), e.getResponseBodyAsString()))
                .block();

        return response.get("candidates").get(0).get("content").get("parts").get(0).get("text").asText();
    }

    private Flux<String> streamGenerateAnswer(String question, String context) {
        var body = mapper.createObjectNode();
        var contents = body.putArray("contents");
        var content = contents.addObject();
        var parts = content.putArray("parts");
        parts.addObject().put("text", buildPrompt(question, context));

        return webClient.post()
                .uri("/models/" + chatModel + ":streamGenerateContent?alt=sse&key=" + apiKey)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body.toString())
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .map(ServerSentEvent::data)
                .filter(Objects::nonNull)
                .mapNotNull(this::extractDeltaText)
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException wcre && isRateLimited(wcre)) {
                        log.warn("Gemini rate limit hit during streaming generation");
                        return Flux.just(RATE_LIMITED_MESSAGE);
                    }
                    log.error("Gemini streaming call failed", e);
                    return Flux.just("[Error streaming response: " + e.getMessage() + "]");
                });
    }

    private String extractDeltaText(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            JsonNode candidates = node.get("candidates");
            if (candidates == null || !candidates.isArray() || candidates.isEmpty()) return null;
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                return parts.get(0).path("text").asText("");
            }
        } catch (Exception e) {
            log.debug("Skipping malformed SSE frame: {}", e.getMessage());
        }
        return null;
    }

    private JsonNode parseJson(String s) {
        try {
            return mapper.readTree(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
