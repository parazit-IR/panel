package com.parazit.panel.domain.provisioning.outbox.repository;

import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxType;
import com.parazit.panel.domain.repository.UuidRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProvisioningOutboxRepository extends UuidRepository<ProvisioningOutbox> {

    Optional<ProvisioningOutbox> findByEventId(UUID eventId);

    Optional<ProvisioningOutbox> findByOrderIdAndType(UUID orderId, ProvisioningOutboxType type);

    List<ProvisioningOutbox> findAvailableForProcessing(Instant now, int limit);

    List<ProvisioningOutbox> findStaleProcessing(Instant staleBefore, int limit);

    List<ProvisioningOutbox> findAllOrderByCreatedAtDesc(int limit);

    boolean existsByOrderIdAndType(UUID orderId, ProvisioningOutboxType type);
}
