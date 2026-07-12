package com.parazit.panel.infrastructure.persistence.payment;

import com.parazit.panel.domain.payment.PaymentOperation;
import com.parazit.panel.domain.payment.repository.PaymentOperationRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentOperationRepositoryAdapter extends JpaRepositoryAdapter<PaymentOperation, UUID>
        implements PaymentOperationRepository {

    private final SpringDataPaymentOperationRepository repository;

    public PaymentOperationRepositoryAdapter(SpringDataPaymentOperationRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public PaymentOperation save(PaymentOperation operation) {
        return repository.saveAndFlush(Objects.requireNonNull(operation, "operation must not be null"));
    }

    @Override
    public List<PaymentOperation> findAllByPaymentIdOrderByOccurredAtAsc(UUID paymentId) {
        return repository.findAllByPaymentIdOrderByOccurredAtAsc(
                Objects.requireNonNull(paymentId, "paymentId must not be null")
        );
    }
}
