package com.parazit.panel.application.renewal;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.config.properties.RenewalWorkerProperties;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.RenewalSnapshot;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.renewal.RenewalFailureClass;
import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FailRenewalOutboxTransaction {

    private final RenewalOutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RenewalNotificationPublisher notificationPublisher;
    private final RenewalWorkerProperties properties;
    private final SystemClockPort clock;

    public FailRenewalOutboxTransaction(
            RenewalOutboxRepository outboxRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            RenewalNotificationPublisher notificationPublisher,
            RenewalWorkerProperties properties,
            SystemClockPort clock
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher, "notificationPublisher must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public void fail(UUID outboxId, RenewalFailure failure) {
        RenewalOutbox outbox = outboxRepository.findById(Objects.requireNonNull(outboxId, "outboxId must not be null"))
                .orElseThrow(() -> new IllegalStateException("Renewal outbox not found"));
        Order order = orderRepository.findById(outbox.getRenewalOrderId())
                .orElseThrow(() -> new IllegalStateException("Renewal order not found"));
        Instant now = clock.now();
        if (failure.retryable() && outbox.getAttempts() < properties.maxAttempts()) {
            outbox.markRetry(failure.code(), now.plus(backoff(outbox.getAttempts())), now);
        } else {
            outbox.markDead(failure.code(), now);
            order.markRenewalExecutionReviewRequired(failure.code(), "Renewal requires manual review", now);
            publishFailure(order, failure, now);
        }
        orderRepository.save(order);
        outboxRepository.save(outbox);
    }

    @Transactional
    public void recoverStale(UUID outboxId) {
        RenewalOutbox outbox = outboxRepository.findById(Objects.requireNonNull(outboxId, "outboxId must not be null"))
                .orElseThrow(() -> new IllegalStateException("Renewal outbox not found"));
        if (!outbox.isProcessingStale(clock.now(), properties.lockTimeout())) {
            return;
        }
        fail(outboxId, new RenewalFailure(RenewalFailureClass.TRANSIENT, "PROCESSING_STALE", true));
    }

    private void publishFailure(Order order, RenewalFailure failure, Instant now) {
        User user = userRepository.findById(order.getUserId()).orElse(null);
        RenewalSnapshot snapshot = order.getRenewalSnapshot();
        if (user == null || snapshot == null) {
            return;
        }
        notificationPublisher.publishAfterCommit(new RenewalFailedNotificationEvent(
                user.getId(),
                user.getTelegramUserId(),
                order.getId(),
                snapshot.targetSubscriptionId(),
                snapshot.serviceDisplayName(),
                snapshot.serviceUsername(),
                failure.code(),
                now
        ));
    }

    private Duration backoff(int attemptCount) {
        double multiplier = Math.pow(properties.backoffMultiplier(), Math.max(0, attemptCount - 1));
        long millis = Math.round(properties.initialBackoff().toMillis() * multiplier);
        long bounded = Math.min(millis, properties.maxBackoff().toMillis());
        return Duration.ofMillis(Math.max(1, bounded));
    }
}
