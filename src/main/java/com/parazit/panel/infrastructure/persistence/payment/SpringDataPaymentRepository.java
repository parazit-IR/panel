package com.parazit.panel.infrastructure.persistence.payment;

import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataPaymentRepository extends SpringDataUuidRepository<Payment> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<Payment> findFirstByWalletTopUpRequestIdOrderByCreatedAtDesc(UUID walletTopUpRequestId);

    Optional<Payment> findByWalletTransactionId(UUID walletTransactionId);

    List<Payment> findAllByOrderIdOrderByCreatedAtDesc(UUID orderId);

    List<Payment> findAllByWalletTopUpRequestIdOrderByCreatedAtDesc(UUID walletTopUpRequestId);

    List<Payment> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Payment> findAllByStatusOrderByExpiresAtAsc(PaymentStatus status);

    boolean existsByOrderIdAndStatus(UUID orderId, PaymentStatus status);

    boolean existsByWalletTopUpRequestIdAndStatus(UUID walletTopUpRequestId, PaymentStatus status);

    long countByUserIdAndStatusIn(UUID userId, List<PaymentStatus> statuses);
}
