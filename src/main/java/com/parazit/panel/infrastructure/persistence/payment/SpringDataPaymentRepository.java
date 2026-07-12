package com.parazit.panel.infrastructure.persistence.payment;

import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataPaymentRepository extends SpringDataUuidRepository<Payment> {

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Payment> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Payment> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Payment> findAllByStatusOrderByExpiresAtAsc(PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
