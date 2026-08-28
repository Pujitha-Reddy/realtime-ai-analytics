package com.example.analytics.controller;

import com.example.analytics.model.OperationalEvent;
import com.example.analytics.pipeline.IngestionPipeline;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class IngestController {

    private final IngestionPipeline pipeline;

    public IngestController(IngestionPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Accepts a CSV with header: category,region,amount,status[,timestamp]
     * timestamp is optional ISO-8601; if omitted, current time is used.
     * Each row is fed through the exact same pipeline as the simulator,
     * so it updates Redis aggregates, trends, alerts, and the live dashboard.
     */
    @PostMapping("/api/ingest/csv")
    public Map<String, Object> ingestCsv(@RequestParam("file") MultipartFile file) throws Exception {
        int accepted = 0;
        int rejected = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                return Map.of("error", "Empty file");
            }
            String[] columns = header.split(",");
            Map<String, Integer> colIndex = new HashMap<>();
            for (int i = 0; i < columns.length; i++) {
                colIndex.put(columns[i].trim().toLowerCase(), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] fields = line.split(",");
                try {
                    String category = fields[colIndex.get("category")].trim();
                    String region = fields[colIndex.get("region")].trim();
                    double amount = Double.parseDouble(fields[colIndex.get("amount")].trim());
                    String status = fields[colIndex.get("status")].trim().toUpperCase();
                    Instant timestamp = colIndex.containsKey("timestamp")
                            ? Instant.parse(fields[colIndex.get("timestamp")].trim())
                            : Instant.now();

                    OperationalEvent event = new OperationalEvent(
                            UUID.randomUUID().toString(), category, region, amount, status, timestamp);
                    pipeline.process(event);
                    accepted++;
                } catch (Exception rowError) {
                    rejected++;
                }
            }
        }

        return Map.of("accepted", accepted, "rejected", rejected);
    }
}
