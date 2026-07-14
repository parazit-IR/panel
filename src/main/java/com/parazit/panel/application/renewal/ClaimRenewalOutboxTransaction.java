package com.parazit.panel.application.renewal;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.config.properties.RenewalWorkerProperties;
import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClaimRenewalOutboxTransaction {

    private final RenewalOutboxRepository outboxRepository;
    private final RenewalWorkerProperties properties;
    private final RenewalMetrics metrics;
    private final SystemClockPort clock;

    public ClaimRenewalOutboxTransaction(
            RenewalOutboxRepository outboxRepository,
            RenewalWorkerProperties properties,
            RenewalMetrics metrics,
            SystemClockPort clock
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public Optional<RenewalOutbox> claim(UUID outboxId) {
        Objects.requireNonNull(outboxId, "outboxId must not be null");
        Optional<RenewalOutbox> claimed = outboxRepository.claimAvailableById(outboxId, clock.now(), properties.workerId());
        metrics.renewalOutboxClaim(claimed.isPresent() ? "claimed" : "miss");
        return claimed;
    }
}
