package com.parazit.panel.infrastructure.persistence.payment;

import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentRepositoryAdapter extends JpaRepositoryAdapter<Payment, UUID> implements PaymentRepository {

    private final SpringDataPaymentRepository repository;

    public PaymentRepositoryAdapter(SpringDataPaymentRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Payment save(Payment payment) {
        return repository.saveAndFlush(Objects.requireNonNull(payment, "payment must not be null"));
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return repository.findFirstByOrderIdOrderByCreatedAtDesc(
                Objects.requireNonNull(orderId, "orderId must not be null")
        );
    }

    @Override
    public List<Payment> findAllByOrderId(UUID orderId) {
        return repository.findAllByOrderIdOrderByCreatedAtDesc(
                Objects.requireNonNull(orderId, "orderId must not be null")
        );
    }

    @Override
    public List<Payment> findAllByUserId(UUID userId) {
        return repository.findAllByUserIdOrderByCreatedAtDesc(
                Objects.requireNonNull(userId, "userId must not be null")
        );
    }

    @Override
    public List<Payment> findWaitingPayments() {
        return repository.findAllByStatusOrderByExpiresAtAsc(PaymentStatus.WAITING_FOR_PAYMENT);
    }

    @Override
    public boolean existsApprovedPaymentForOrder(UUID orderId) {
        return repository.existsByOrderIdAndStatus(
                Objects.requireNonNull(orderId, "orderId must not be null"),
                PaymentStatus.APPROVED
        );
    }
}
