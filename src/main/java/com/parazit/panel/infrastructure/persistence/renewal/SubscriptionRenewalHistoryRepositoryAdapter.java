package com.parazit.panel.infrastructure.persistence.renewal;

import com.parazit.panel.domain.renewal.SubscriptionRenewalHistory;
import com.parazit.panel.domain.renewal.repository.SubscriptionRenewalHistoryRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionRenewalHistoryRepositoryAdapter
        extends JpaRepositoryAdapter<SubscriptionRenewalHistory, UUID>
        implements SubscriptionRenewalHistoryRepository {

    private final SpringDataSubscriptionRenewalHistoryRepository repository;

    public SubscriptionRenewalHistoryRepositoryAdapter(SpringDataSubscriptionRenewalHistoryRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public SubscriptionRenewalHistory save(SubscriptionRenewalHistory history) {
        return repository.saveAndFlush(Objects.requireNonNull(history, "history must not be null"));
    }

    @Override
    public Optional<SubscriptionRenewalHistory> findByRenewalOrderId(UUID renewalOrderId) {
        return repository.findByRenewalOrderId(Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null"));
    }

    @Override
    public boolean existsByRenewalOrderId(UUID renewalOrderId) {
        return repository.existsByRenewalOrderId(Objects.requireNonNull(renewalOrderId, "renewalOrderId must not be null"));
    }
}
