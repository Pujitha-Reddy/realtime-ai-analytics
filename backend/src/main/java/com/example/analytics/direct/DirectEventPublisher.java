package com.example.analytics.direct;

import com.example.analytics.pipeline.IngestionPipeline;
import com.example.analytics.service.SimulatorControlService;
import com.example.analytics.util.EventFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class DirectEventPublisher {

    private final IngestionPipeline pipeline;
    private final SimulatorControlService control;

    public DirectEventPublisher(IngestionPipeline pipeline, SimulatorControlService control) {
        this.pipeline = pipeline;
        this.control = control;
    }

    @Scheduled(fixedDelay = 300)
    public void publishEvent() {
        if (control.isPaused()) return;
        pipeline.process(EventFactory.random());
    }
}
