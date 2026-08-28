package com.example.analytics.kafka;

import com.example.analytics.service.SimulatorControlService;
import com.example.analytics.util.EventFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class KafkaEventProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SimulatorControlService control;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final String topic;

    public KafkaEventProducer(KafkaTemplate<String, String> kafkaTemplate,
                               SimulatorControlService control,
                               @Value("${app.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.control = control;
        this.topic = topic;
    }

    @Scheduled(fixedDelay = 300)
    public void publishEvent() {
        if (control.isPaused()) return;
        var event = EventFactory.random();
        try {
            kafkaTemplate.send(topic, event.id(), mapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish event {} to Kafka", event.id(), e);
        }
    }
}
