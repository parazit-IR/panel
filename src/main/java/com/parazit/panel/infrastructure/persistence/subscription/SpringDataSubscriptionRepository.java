package com.parazit.panel.infrastructure.persistence.subscription;

import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.SubscriptionStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSubscriptionRepository extends SpringDataUuidRepository<Subscription> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select subscription from Subscription subscription where subscription.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") UUID id);

    Optional<Subscription> findByXuiClientProvisionId(UUID provisionId);

    Optional<Subscription> findByAccessTokenHash(String tokenHash);

    Optional<Subscription> findByUserIdAndId(UUID userId, UUID subscriptionId);

    List<Subscription> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, SubscriptionStatus status);

    boolean existsByXuiClientProvisionId(UUID provisionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Subscription subscription
            set subscription.lastAccessedAt = :accessedAt,
                subscription.accessCount = subscription.accessCount + 1
            where subscription.id = :subscriptionId
            """)
    int incrementAccessMetadata(
            @Param("subscriptionId") UUID subscriptionId,
            @Param("accessedAt") Instant accessedAt
    );
}
