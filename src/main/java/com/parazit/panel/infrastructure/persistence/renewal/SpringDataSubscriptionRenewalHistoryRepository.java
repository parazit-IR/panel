package com.parazit.panel.infrastructure.persistence.renewal;

import com.parazit.panel.domain.renewal.SubscriptionRenewalHistory;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSubscriptionRenewalHistoryRepository
        extends SpringDataUuidRepository<SubscriptionRenewalHistory> {

    Optional<SubscriptionRenewalHistory> findByRenewalOrderId(UUID renewalOrderId);

    boolean existsByRenewalOrderId(UUID renewalOrderId);
}
