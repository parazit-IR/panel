package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClaimProvisioningOutboxTransaction {

    private final ProvisioningOutboxRepository outboxRepository;
    private final OrderRepository orderRepository;
    private final SystemClockPort clock;

    public ClaimProvisioningOutboxTransaction(
            ProvisioningOutboxRepository outboxRepository,
            OrderRepository orderRepository,
            SystemClockPort clock
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public Optional<ProvisioningOutbox> claim(UUID eventId) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Instant now = clock.now();
        Optional<ProvisioningOutbox> claimed = outboxRepository.claimAvailableByEventId(eventId, now);
        if (claimed.isEmpty()) {
            if (outboxRepository.findByEventId(eventId).isEmpty()) {
                throw new ProvisioningOutboxNotFoundException(eventId);
            }
            return Optional.empty();
        }
        ProvisioningOutbox outbox = claimed.orElseThrow();
        Order order = orderRepository.findById(outbox.getOrderId())
                .orElseThrow(() -> new ProvisioningOutboxException("Provisioning order could not be found"));
        order.markProvisioning(now);
        orderRepository.save(order);
        return Optional.of(outbox);
    }
}
