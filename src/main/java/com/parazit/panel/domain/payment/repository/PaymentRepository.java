package com.parazit.panel.domain.payment.repository;

import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends UuidRepository<Payment> {

    Optional<Payment> findByOrderId(UUID orderId);

    List<Payment> findAllByOrderId(UUID orderId);

    List<Payment> findAllByUserId(UUID userId);

    List<Payment> findWaitingPayments();

    boolean existsApprovedPaymentForOrder(UUID orderId);
}
