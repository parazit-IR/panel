package com.parazit.panel.domain.renewal.repository;

import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.Optional;
import java.util.UUID;

public interface RenewalOutboxRepository extends UuidRepository<RenewalOutbox> {

    Optional<RenewalOutbox> findByRenewalOrderIdAndEventType(UUID renewalOrderId, String eventType);

    Optional<RenewalOutbox> findByPaymentId(UUID paymentId);

    boolean existsByRenewalOrderIdAndEventType(UUID renewalOrderId, String eventType);
}
