package com.parazit.panel.domain.renewal.repository;

import com.parazit.panel.domain.renewal.SubscriptionRenewalHistory;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRenewalHistoryRepository extends UuidRepository<SubscriptionRenewalHistory> {

    Optional<SubscriptionRenewalHistory> findByRenewalOrderId(UUID renewalOrderId);

    boolean existsByRenewalOrderId(UUID renewalOrderId);
}
