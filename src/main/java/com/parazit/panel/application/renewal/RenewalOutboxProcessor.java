package com.parazit.panel.application.renewal;

import com.parazit.panel.application.port.in.renewal.ApplyRenewalUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.renewal.command.ApplyRenewalCommand;
import com.parazit.panel.config.properties.RenewalWorkerProperties;
import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RenewalOutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(RenewalOutboxProcessor.class);

    private final RenewalOutboxRepository outboxRepository;
    private final ClaimRenewalOutboxTransaction claimTransaction;
    private final ApplyRenewalUseCase applyRenewalUseCase;
    private final FailRenewalOutboxTransaction failTransaction;
    private final RenewalFailureClassifier failureClassifier;
    private final RenewalMetrics metrics;
    private final RenewalWorkerProperties properties;
    private final SystemClockPort clock;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public RenewalOutboxProcessor(
            RenewalOutboxRepository outboxRepository,
            ClaimRenewalOutboxTransaction claimTransaction,
            ApplyRenewalUseCase applyRenewalUseCase,
            FailRenewalOutboxTransaction failTransaction,
            RenewalFailureClassifier failureClassifier,
            RenewalMetrics metrics,
            RenewalWorkerProperties properties,
            SystemClockPort clock
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.claimTransaction = Objects.requireNonNull(claimTransaction, "claimTransaction must not be null");
        this.applyRenewalUseCase = Objects.requireNonNull(applyRenewalUseCase, "applyRenewalUseCase must not be null");
        this.failTransaction = Objects.requireNonNull(failTransaction, "failTransaction must not be null");
        this.failureClassifier = Objects.requireNonNull(failureClassifier, "failureClassifier must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public int processAvailable() {
        if (!running.compareAndSet(false, true)) {
            return 0;
        }
        try {
            recoverStaleProcessing();
            List<RenewalOutbox> available = outboxRepository.findAvailableForProcessing(clock.now(), properties.batchSize());
            int processed = 0;
            for (RenewalOutbox candidate : available) {
                if (processOne(candidate.getId())) {
                    processed++;
                }
            }
            metrics.renewalWorkerPoll(processed > 0 ? "processed" : "empty");
            return processed;
        } finally {
            running.set(false);
        }
    }

    public boolean processOne(UUID outboxId) {
        return claimTransaction.claim(outboxId)
                .map(this::executeClaimed)
                .orElse(false);
    }

    private boolean executeClaimed(RenewalOutbox outbox) {
        try {
            applyRenewalUseCase.apply(new ApplyRenewalCommand(
                    outbox.getId(),
                    outbox.getRenewalOrderId(),
                    outbox.getTargetSubscriptionId(),
                    outbox.getTargetProvisionId(),
                    outbox.getId()
            ));
            metrics.renewalApply("success", null);
            log.atInfo()
                    .addKeyValue("renewalOutboxId", outbox.getId())
                    .addKeyValue("renewalOrderId", outbox.getRenewalOrderId())
                    .log("Renewal outbox processed");
            return true;
        } catch (RuntimeException exception) {
            RenewalFailure failure = failureClassifier.classify(exception);
            failTransaction.fail(outbox.getId(), failure);
            if (failure.retryable()) {
                metrics.renewalRetryScheduled(failure.failureClass().name());
            } else {
                metrics.renewalPermanentFailure(failure.failureClass().name());
            }
            log.atWarn()
                    .addKeyValue("renewalOutboxId", outbox.getId())
                    .addKeyValue("renewalOrderId", outbox.getRenewalOrderId())
                    .addKeyValue("failureCode", failure.code())
                    .addKeyValue("retryable", failure.retryable())
                    .log("Renewal outbox processing failed");
            return false;
        }
    }

    private void recoverStaleProcessing() {
        outboxRepository.findStaleProcessing(clock.now().minus(properties.lockTimeout()), properties.batchSize())
                .forEach(outbox -> {
                    log.atWarn()
                            .addKeyValue("renewalOutboxId", outbox.getId())
                            .addKeyValue("renewalOrderId", outbox.getRenewalOrderId())
                            .log("Recovering stale renewal outbox");
                    failTransaction.recoverStale(outbox.getId());
                });
    }
}
