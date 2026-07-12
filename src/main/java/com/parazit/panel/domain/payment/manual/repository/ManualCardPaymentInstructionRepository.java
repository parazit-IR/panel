package com.parazit.panel.domain.payment.manual.repository;

import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManualCardPaymentInstructionRepository extends UuidRepository<ManualCardPaymentInstruction> {

    Optional<ManualCardPaymentInstruction> findByInstructionRequestId(UUID instructionRequestId);

    Optional<ManualCardPaymentInstruction> findActiveByPaymentId(UUID paymentId);

    Optional<ManualCardPaymentInstruction> findActiveByCurrencyAndPayableAmount(String currency, long payableAmount);

    List<ManualCardPaymentInstruction> findAllByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    boolean existsActiveByPaymentId(UUID paymentId);
}
