package com.parazit.panel.infrastructure.persistence.renewal;

import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RenewalOutboxRepositoryAdapter
        extends JpaRepositoryAdapter<RenewalOutbox, UUID>
        implements RenewalOutboxRepository {

    private final SpringDataRenewalOutboxRepository repository;

    public RenewalOutboxRepositoryAdapter(SpringDataRenewalOutboxRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public RenewalOutbox save(RenewalOutbox outbox) {
        return repository.saveAndFlush(Objects.requireNonNull(outbox, "outbox must not be null"));
    }

    @Override
    public Optional<RenewalOutbox> findByRenewalOrderIdAndEventType(UUID renewalOrderId, String eventType) {
        return repository.findByRenewalOrderIdAndEventType(
                Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null"),
                requireText(eventType, "eventType")
        );
    }

    @Override
    public Optional<RenewalOutbox> findByPaymentId(UUID paymentId) {
        return repository.findByPaymentId(Objects.requireNonNull(paymentId, "paymentId must not be null"));
    }

    @Override
    public boolean existsByRenewalOrderIdAndEventType(UUID renewalOrderId, String eventType) {
        return repository.existsByRenewalOrderIdAndEventType(
                Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null"),
                requireText(eventType, "eventType")
        );
    }

    private static String requireText(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
