package com.parazit.panel.infrastructure.persistence.renewal;

import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.RenewalOutboxStatus;
import com.parazit.panel.domain.renewal.repository.RenewalOutboxRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class RenewalOutboxRepositoryAdapter
        extends JpaRepositoryAdapter<RenewalOutbox, UUID>
        implements RenewalOutboxRepository {

    private final SpringDataRenewalOutboxRepository repository;
    private final EntityManager entityManager;

    public RenewalOutboxRepositoryAdapter(SpringDataRenewalOutboxRepository repository, EntityManager entityManager) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager must not be null");
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
    public Optional<RenewalOutbox> claimAvailableById(UUID id, Instant now, String workerId) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(now, "now must not be null");
        String normalizedWorkerId = requireText(workerId, "workerId");
        List<?> results = entityManager.createNativeQuery("""
                        WITH candidate AS (
                            SELECT id
                            FROM renewal_outbox
                            WHERE id = :id
                              AND status IN ('PENDING', 'FAILED')
                              AND available_at <= :now
                            ORDER BY available_at ASC
                            FOR UPDATE SKIP LOCKED
                            LIMIT 1
                        )
                        UPDATE renewal_outbox outbox
                        SET status = 'PROCESSING',
                            locked_at = :now,
                            locked_by = :workerId,
                            attempts = attempts + 1,
                            last_error_code = NULL,
                            updated_at = :now
                        FROM candidate
                        WHERE outbox.id = candidate.id
                        RETURNING outbox.*
                        """, RenewalOutbox.class)
                .setParameter("id", id)
                .setParameter("now", now)
                .setParameter("workerId", normalizedWorkerId)
                .getResultList();
        return results.stream()
                .map(RenewalOutbox.class::cast)
                .findFirst();
    }

    @Override
    public List<RenewalOutbox> findAvailableForProcessing(Instant now, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repository.findAllByStatusInAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
                List.of(RenewalOutboxStatus.PENDING, RenewalOutboxStatus.FAILED),
                Objects.requireNonNull(now, "now must not be null"),
                PageRequest.of(0, safeLimit)
        );
    }

    @Override
    public List<RenewalOutbox> findStaleProcessing(Instant staleBefore, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repository.findAllByStatusAndLockedAtLessThanEqualOrderByLockedAtAsc(
                RenewalOutboxStatus.PROCESSING,
                Objects.requireNonNull(staleBefore, "staleBefore must not be null"),
                PageRequest.of(0, safeLimit)
        );
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
