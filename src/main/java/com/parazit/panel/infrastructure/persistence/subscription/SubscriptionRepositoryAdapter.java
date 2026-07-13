package com.parazit.panel.infrastructure.persistence.subscription;

import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SubscriptionRepositoryAdapter
        extends JpaRepositoryAdapter<Subscription, UUID>
        implements SubscriptionRepository {

    private final SpringDataSubscriptionRepository repository;

    public SubscriptionRepositoryAdapter(SpringDataSubscriptionRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Subscription save(Subscription subscription) {
        return repository.saveAndFlush(Objects.requireNonNull(subscription, "subscription must not be null"));
    }

    @Override
    public Optional<Subscription> findByIdForUpdate(UUID id) {
        return repository.findByIdForUpdate(Objects.requireNonNull(id, "id must not be null"));
    }

    @Override
    public Optional<Subscription> findByXuiClientProvisionId(UUID provisionId) {
        return repository.findByXuiClientProvisionId(Objects.requireNonNull(provisionId, "provisionId must not be null"));
    }

    @Override
    public Optional<Subscription> findByAccessTokenHash(String tokenHash) {
        return repository.findByAccessTokenHash(Objects.requireNonNull(tokenHash, "tokenHash must not be null"));
    }

    @Override
    public Optional<Subscription> findByUserIdAndId(UUID userId, UUID subscriptionId) {
        return repository.findByUserIdAndId(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(subscriptionId, "subscriptionId must not be null")
        );
    }

    @Override
    public List<Subscription> findAllByUserIdOrderByCreatedAtDesc(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(Objects.requireNonNull(userId, "userId must not be null"));
    }

    @Override
    public boolean existsByXuiClientProvisionId(UUID provisionId) {
        return repository.existsByXuiClientProvisionId(Objects.requireNonNull(provisionId, "provisionId must not be null"));
    }

    @Override
    @Transactional
    public boolean incrementAccessMetadata(UUID subscriptionId, Instant accessedAt) {
        int updated = repository.incrementAccessMetadata(
                Objects.requireNonNull(subscriptionId, "subscriptionId must not be null"),
                Objects.requireNonNull(accessedAt, "accessedAt must not be null")
        );
        return updated == 1;
    }
}
