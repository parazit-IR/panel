package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.config.properties.ProvisioningOutboxProperties;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FailProvisioningOutboxTransaction {

    private final ProvisioningOutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final ProvisioningOutboxProperties properties;
    private final SystemClockPort clock;

    public FailProvisioningOutboxTransaction(
            ProvisioningOutboxRepository outboxRepository,
            OrderRepository orderRepository,
            ProvisioningOutboxProperties properties,
            SystemClockPort clock
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public void fail(UUID eventId, ProvisioningFailure failure) {
        ProvisioningOutbox outbox = outboxRepository.findByEventId(eventId)
                .orElseThrow(() -> new ProvisioningOutboxNotFoundException(eventId));
        Order order = orderRepository.findById(outbox.getOrderId())
                .orElseThrow(() -> new ProvisioningOutboxException("Provisioning order could not be found"));
        Instant now = clock.now();
        if (!failure.retryable() || outbox.getAttemptCount() + 1 >= properties.maxAttempts()) {
            outbox.markDead(failure.code(), failure.message(), now);
        } else if (failure.unknown()) {
            outbox.markUnknown(failure.code(), failure.message(), now, now.plus(backoff(outbox.getAttemptCount())));
        } else {
            outbox.markFailed(failure.code(), failure.message(), now, now.plus(backoff(outbox.getAttemptCount())));
        }
        order.markProvisioningFailed(failure.code(), failure.message());
        orderRepository.save(order);
        outboxRepository.save(outbox);
    }

    @Transactional
    public void recoverStale(UUID eventId) {
        ProvisioningOutbox outbox = outboxRepository.findByEventId(eventId)
                .orElseThrow(() -> new ProvisioningOutboxNotFoundException(eventId));
        if (!outbox.isProcessingStale(clock.now(), properties.processingTimeout())) {
            return;
        }
        fail(eventId, new ProvisioningFailure(true, true, "PROCESSING_STALE", "Provisioning processing timed out"));
    }

    private Duration backoff(int priorAttempts) {
        double multiplier = Math.pow(properties.retryMultiplier(), Math.max(0, priorAttempts));
        long millis = Math.round(properties.initialRetryDelay().toMillis() * multiplier);
        long bounded = Math.min(millis, properties.maxRetryDelay().toMillis());
        return Duration.ofMillis(Math.max(1, bounded));
    }
}
