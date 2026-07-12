package com.parazit.panel.infrastructure.scheduling;

import com.parazit.panel.application.provisioning.outbox.ProvisioningOutboxProcessor;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.provisioning.outbox", name = "enabled", havingValue = "true")
public class ProvisioningOutboxScheduler {

    private final ProvisioningOutboxProcessor processor;

    public ProvisioningOutboxScheduler(ProvisioningOutboxProcessor processor) {
        this.processor = Objects.requireNonNull(processor, "processor must not be null");
    }

    @Scheduled(fixedDelayString = "${app.provisioning.outbox.poll-interval:PT5S}")
    public void poll() {
        processor.processAvailable();
    }
}
