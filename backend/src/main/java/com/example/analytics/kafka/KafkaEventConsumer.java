package com.example.analytics.kafka;

import com.example.analytics.model.OperationalEvent;
import com.example.analytics.pipeline.IngestionPipeline;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final IngestionPipeline pipeline;

    public KafkaEventConsumer(IngestionPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @KafkaListener(topics = "${app.kafka.topic}")
    public void consume(String message) {
        try {
            OperationalEvent event = mapper.readValue(message, OperationalEvent.class);
            pipeline.process(event);
        } catch (Exception e) {
            // A malformed message shouldn't crash the consumer or stall the
            // partition — log it and move on. In a stricter production setup
            // this would publish to a dead-letter topic for later inspection.
            log.error("Failed to process Kafka message, skipping: {}", message, e);
        }
    }
}
