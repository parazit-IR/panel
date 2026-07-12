package com.parazit.panel.infrastructure.persistence.provisioning.outbox;

import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxStatus;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxType;
import com.parazit.panel.domain.provisioning.outbox.repository.ProvisioningOutboxRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class ProvisioningOutboxRepositoryAdapter
        extends JpaRepositoryAdapter<ProvisioningOutbox, UUID>
        implements ProvisioningOutboxRepository {

    private static final List<ProvisioningOutboxStatus> AVAILABLE_STATUSES = List.of(
            ProvisioningOutboxStatus.PENDING,
            ProvisioningOutboxStatus.FAILED,
            ProvisioningOutboxStatus.UNKNOWN
    );

    private final SpringDataProvisioningOutboxRepository repository;

    public ProvisioningOutboxRepositoryAdapter(SpringDataProvisioningOutboxRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public ProvisioningOutbox save(ProvisioningOutbox outbox) {
        return repository.saveAndFlush(Objects.requireNonNull(outbox, "outbox must not be null"));
    }

    @Override
    public Optional<ProvisioningOutbox> findByEventId(UUID eventId) {
        return repository.findByEventId(Objects.requireNonNull(eventId, "eventId must not be null"));
    }

    @Override
    public Optional<ProvisioningOutbox> findByOrderIdAndType(UUID orderId, ProvisioningOutboxType type) {
        return repository.findByOrderIdAndType(
                Objects.requireNonNull(orderId, "orderId must not be null"),
                Objects.requireNonNull(type, "type must not be null")
        );
    }

    @Override
    public List<ProvisioningOutbox> findAvailableForProcessing(Instant now, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repository.findAllByStatusInAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
                AVAILABLE_STATUSES,
                Objects.requireNonNull(now, "now must not be null"),
                PageRequest.of(0, safeLimit)
        );
    }

    @Override
    public List<ProvisioningOutbox> findStaleProcessing(Instant staleBefore, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repository.findAllByStatusAndProcessingStartedAtLessThanEqualOrderByProcessingStartedAtAsc(
                ProvisioningOutboxStatus.PROCESSING,
                Objects.requireNonNull(staleBefore, "staleBefore must not be null"),
                PageRequest.of(0, safeLimit)
        );
    }

    @Override
    public List<ProvisioningOutbox> findAllOrderByCreatedAtDesc(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit));
    }

    @Override
    public boolean existsByOrderIdAndType(UUID orderId, ProvisioningOutboxType type) {
        return repository.existsByOrderIdAndType(
                Objects.requireNonNull(orderId, "orderId must not be null"),
                Objects.requireNonNull(type, "type must not be null")
        );
    }
}
