package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
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
        ProvisioningOutbox outbox = outboxRepository.findByEventId(eventId)
                .orElseThrow(() -> new ProvisioningOutboxNotFoundException(eventId));
        if (!outbox.isAvailable(clock.now())) {
            return Optional.empty();
        }
        Order order = orderRepository.findById(outbox.getOrderId())
                .orElseThrow(() -> new ProvisioningOutboxException("Provisioning order could not be found"));
        outbox.markProcessing(clock.now());
        order.markProvisioning(clock.now());
        orderRepository.save(order);
        return Optional.of(outboxRepository.save(outbox));
    }
}
