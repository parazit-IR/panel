package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.config.properties.ProvisioningOutboxProperties;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProvisioningOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(ProvisioningOutboxProcessor.class);

    private final ProvisioningOutboxRepository outboxRepository;
    private final ClaimProvisioningOutboxTransaction claimTransaction;
    private final CompleteProvisioningOutboxTransaction completeTransaction;
    private final FailProvisioningOutboxTransaction failTransaction;
    private final CreateVpnClientOutboxHandler createVpnClientHandler;
    private final ProvisioningFailureClassifier failureClassifier;
    private final ProvisioningOutboxProperties properties;
    private final SystemClockPort clock;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ProvisioningOutboxProcessor(
            ProvisioningOutboxRepository outboxRepository,
            ClaimProvisioningOutboxTransaction claimTransaction,
            CompleteProvisioningOutboxTransaction completeTransaction,
            FailProvisioningOutboxTransaction failTransaction,
            CreateVpnClientOutboxHandler createVpnClientHandler,
            ProvisioningFailureClassifier failureClassifier,
            ProvisioningOutboxProperties properties,
            SystemClockPort clock
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.claimTransaction = Objects.requireNonNull(claimTransaction, "claimTransaction must not be null");
        this.completeTransaction = Objects.requireNonNull(completeTransaction, "completeTransaction must not be null");
        this.failTransaction = Objects.requireNonNull(failTransaction, "failTransaction must not be null");
        this.createVpnClientHandler = Objects.requireNonNull(createVpnClientHandler, "createVpnClientHandler must not be null");
        this.failureClassifier = Objects.requireNonNull(failureClassifier, "failureClassifier must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public int processAvailable() {
        if (!running.compareAndSet(false, true)) {
            return 0;
        }
        try {
            recoverStaleProcessing();
            List<ProvisioningOutbox> available = outboxRepository.findAvailableForProcessing(
                    clock.now(),
                    properties.batchSize()
            );
            int processed = 0;
            for (ProvisioningOutbox candidate : available) {
                if (processOne(candidate.getEventId())) {
                    processed++;
                }
            }
            return processed;
        } finally {
            running.set(false);
        }
    }

    public boolean processOne(UUID eventId) {
        return claimTransaction.claim(eventId)
                .map(this::executeClaimed)
                .orElse(false);
    }

    private boolean executeClaimed(ProvisioningOutbox outbox) {
        UUID eventId = outbox.getEventId();
        try {
            createVpnClientHandler.handle(outbox);
            completeTransaction.complete(eventId);
            log.atInfo().addKeyValue("eventId", eventId).log("Provisioning outbox processed");
            return true;
        } catch (RuntimeException exception) {
            ProvisioningFailure failure = failureClassifier.classify(exception);
            failTransaction.fail(eventId, failure);
            log.atWarn()
                    .addKeyValue("eventId", eventId)
                    .addKeyValue("failureCode", failure.code())
                    .addKeyValue("retryable", failure.retryable())
                    .log("Provisioning outbox processing failed");
            return false;
        }
    }

    private void recoverStaleProcessing() {
        outboxRepository.findStaleProcessing(
                clock.now().minus(properties.processingTimeout()),
                properties.batchSize()
        ).forEach(outbox -> {
            log.atWarn().addKeyValue("eventId", outbox.getEventId()).log("Recovering stale provisioning outbox");
            failTransaction.recoverStale(outbox.getEventId());
        });
    }
}
