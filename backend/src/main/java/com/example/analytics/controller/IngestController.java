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
import java.util.Set;
import java.util.UUID;

@RestController
public class IngestController {

    private static final int MAX_ROWS = 5000;
    private static final double MAX_AMOUNT = 1_000_000.0;
    private static final int MAX_FIELD_LENGTH = 100;
    private static final Set<String> VALID_STATUSES = Set.of("SUCCESS", "PENDING", "FAILED");

    private final IngestionPipeline pipeline;

    public IngestController(IngestionPipeline pipeline) {
        this.pipeline = pipeline;
    }

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
            if (!colIndex.containsKey("category") || !colIndex.containsKey("region")
                    || !colIndex.containsKey("amount") || !colIndex.containsKey("status")) {
                return Map.of("error", "CSV must have category, region, amount, status columns");
            }

            String line;
            int rowCount = 0;
            while ((line = reader.readLine()) != null && rowCount < MAX_ROWS) {
                rowCount++;
                if (line.isBlank()) continue;
                String[] fields = line.split(",");
                try {
                    String category = sanitizeField(fields[colIndex.get("category")]);
                    String region = sanitizeField(fields[colIndex.get("region")]);
                    double amount = Double.parseDouble(fields[colIndex.get("amount")].trim());
                    String status = fields[colIndex.get("status")].trim().toUpperCase();

                    if (amount < 0 || amount > MAX_AMOUNT) {
                        rejected++;
                        continue;
                    }
                    if (!VALID_STATUSES.contains(status)) {
                        rejected++;
                        continue;
                    }

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

    /** Trims, caps length, and strips characters that have no business in a category/region name. */
    private String sanitizeField(String raw) {
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_FIELD_LENGTH) {
            trimmed = trimmed.substring(0, MAX_FIELD_LENGTH);
        }
        return trimmed.replaceAll("[<>\"']", "");
    }
}
