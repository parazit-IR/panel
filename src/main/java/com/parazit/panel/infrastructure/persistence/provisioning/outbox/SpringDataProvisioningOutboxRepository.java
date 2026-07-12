package com.parazit.panel.infrastructure.persistence.provisioning.outbox;

import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxStatus;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxType;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface SpringDataProvisioningOutboxRepository extends SpringDataUuidRepository<ProvisioningOutbox> {

    Optional<ProvisioningOutbox> findByEventId(UUID eventId);

    Optional<ProvisioningOutbox> findByOrderIdAndType(UUID orderId, ProvisioningOutboxType type);

    List<ProvisioningOutbox> findAllByStatusInAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
            List<ProvisioningOutboxStatus> statuses,
            Instant now,
            Pageable pageable
    );

    List<ProvisioningOutbox> findAllByStatusAndProcessingStartedAtLessThanEqualOrderByProcessingStartedAtAsc(
            ProvisioningOutboxStatus status,
            Instant staleBefore,
            Pageable pageable
    );

    List<ProvisioningOutbox> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByOrderIdAndType(UUID orderId, ProvisioningOutboxType type);
}
