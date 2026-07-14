package com.parazit.panel.domain.payment.repository;

import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends UuidRepository<Payment> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByWalletTopUpRequestId(UUID walletTopUpRequestId);

    Optional<Payment> findByWalletTransactionId(UUID walletTransactionId);

    default Optional<Payment> findByIdForUpdate(UUID id) {
        return findById(id);
    }

    List<Payment> findAllByOrderId(UUID orderId);

    List<Payment> findAllByWalletTopUpRequestId(UUID walletTopUpRequestId);

    List<Payment> findAllByUserId(UUID userId);

    List<Payment> findWaitingPayments();

    boolean existsApprovedPaymentForOrder(UUID orderId);

    boolean existsApprovedPaymentForWalletTopUpRequest(UUID walletTopUpRequestId);
}
