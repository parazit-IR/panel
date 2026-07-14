package com.parazit.panel.infrastructure.scheduling;

import com.parazit.panel.application.renewal.RenewalOutboxProcessor;
import com.parazit.panel.config.properties.RenewalWorkerProperties;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.renewal.worker", name = "enabled", havingValue = "true")
public class RenewalOutboxScheduler {

    private final RenewalOutboxProcessor processor;
    private final RenewalWorkerProperties properties;

    public RenewalOutboxScheduler(RenewalOutboxProcessor processor, RenewalWorkerProperties properties) {
        this.processor = Objects.requireNonNull(processor, "processor must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Scheduled(fixedDelayString = "${app.renewal.worker.poll-interval:PT5S}")
    public void poll() {
        processor.processAvailable();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void processOnStartup() {
        if (properties.processOnStartup()) {
            processor.processAvailable();
        }
    }
}
