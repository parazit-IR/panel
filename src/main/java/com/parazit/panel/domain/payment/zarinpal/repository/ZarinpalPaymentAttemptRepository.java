package com.parazit.panel.domain.payment.zarinpal.repository;

import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ZarinpalPaymentAttemptRepository extends UuidRepository<ZarinpalPaymentAttempt> {

    Optional<ZarinpalPaymentAttempt> findByRequestId(UUID requestId);

    Optional<ZarinpalPaymentAttempt> findByAuthority(String authority);

    Optional<ZarinpalPaymentAttempt> findVerifiedByPaymentId(UUID paymentId);

    List<ZarinpalPaymentAttempt> findAllByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    boolean existsByAuthority(String authority);
}
