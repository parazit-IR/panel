package com.parazit.panel.infrastructure.persistence.payment.zarinpal;

import com.parazit.panel.domain.payment.zarinpal.ZarinpalAttemptStatus;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataZarinpalPaymentAttemptRepository extends SpringDataUuidRepository<ZarinpalPaymentAttempt> {

    Optional<ZarinpalPaymentAttempt> findByRequestId(UUID requestId);

    Optional<ZarinpalPaymentAttempt> findByAuthority(String authority);

    Optional<ZarinpalPaymentAttempt> findFirstByPaymentIdAndStatus(UUID paymentId, ZarinpalAttemptStatus status);

    List<ZarinpalPaymentAttempt> findAllByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    boolean existsByAuthority(String authority);
}
