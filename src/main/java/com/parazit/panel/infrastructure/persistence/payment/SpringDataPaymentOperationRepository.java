package com.parazit.panel.infrastructure.persistence.payment;

import com.parazit.panel.domain.payment.PaymentOperation;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.UUID;

public interface SpringDataPaymentOperationRepository extends SpringDataUuidRepository<PaymentOperation> {

    List<PaymentOperation> findAllByPaymentIdOrderByOccurredAtAsc(UUID paymentId);
}
