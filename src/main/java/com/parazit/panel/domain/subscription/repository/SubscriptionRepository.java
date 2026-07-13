package com.parazit.panel.domain.subscription.repository;

import com.parazit.panel.domain.repository.UuidRepository;
import com.parazit.panel.domain.subscription.Subscription;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends UuidRepository<Subscription> {

    default Optional<Subscription> findByIdForUpdate(UUID id) {
        return findById(id);
    }

    Optional<Subscription> findByXuiClientProvisionId(UUID provisionId);

    Optional<Subscription> findByAccessTokenHash(String tokenHash);

    Optional<Subscription> findByUserIdAndId(UUID userId, UUID subscriptionId);

    List<Subscription> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByXuiClientProvisionId(UUID provisionId);

    boolean incrementAccessMetadata(UUID subscriptionId, Instant accessedAt);
}
