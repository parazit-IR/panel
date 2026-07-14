package com.parazit.panel.infrastructure.persistence.renewal;

import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataRenewalOutboxRepository extends SpringDataUuidRepository<RenewalOutbox> {

    Optional<RenewalOutbox> findByRenewalOrderIdAndEventType(UUID renewalOrderId, String eventType);

    Optional<RenewalOutbox> findByPaymentId(UUID paymentId);

    boolean existsByRenewalOrderIdAndEventType(UUID renewalOrderId, String eventType);
}
