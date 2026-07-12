package com.parazit.panel.domain.payment.repository;

import com.parazit.panel.domain.payment.PaymentOperation;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.UUID;

public interface PaymentOperationRepository extends UuidRepository<PaymentOperation> {

    List<PaymentOperation> findAllByPaymentIdOrderByOccurredAtAsc(UUID paymentId);
}
