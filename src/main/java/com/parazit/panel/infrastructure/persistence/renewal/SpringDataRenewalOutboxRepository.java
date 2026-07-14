package com.parazit.panel.infrastructure.persistence.renewal;

import com.parazit.panel.domain.renewal.RenewalOutbox;
import com.parazit.panel.domain.renewal.RenewalOutboxStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface SpringDataRenewalOutboxRepository extends SpringDataUuidRepository<RenewalOutbox> {

    Optional<RenewalOutbox> findByRenewalOrderIdAndEventType(UUID renewalOrderId, String eventType);

    Optional<RenewalOutbox> findByPaymentId(UUID paymentId);

    List<RenewalOutbox> findAllByStatusInAndAvailableAtLessThanEqualOrderByAvailableAtAsc(
            List<RenewalOutboxStatus> statuses,
            Instant now,
            Pageable pageable
    );

    List<RenewalOutbox> findAllByStatusAndLockedAtLessThanEqualOrderByLockedAtAsc(
            RenewalOutboxStatus status,
            Instant staleBefore,
            Pageable pageable
    );

    boolean existsByRenewalOrderIdAndEventType(UUID renewalOrderId, String eventType);
}
