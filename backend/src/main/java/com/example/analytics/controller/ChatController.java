package com.example.analytics.controller;

import com.example.analytics.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    public record ChatRequest(String question) {}

    @PostMapping
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        return Map.of("answer", ragService.answer(request.question()));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String question) {
        return ragService.streamAnswer(question);
    }
}
