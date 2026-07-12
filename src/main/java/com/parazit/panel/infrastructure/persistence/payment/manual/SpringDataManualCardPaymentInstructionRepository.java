package com.parazit.panel.infrastructure.persistence.payment.manual;

import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataManualCardPaymentInstructionRepository
        extends SpringDataUuidRepository<ManualCardPaymentInstruction> {

    Optional<ManualCardPaymentInstruction> findByInstructionRequestId(UUID instructionRequestId);

    Optional<ManualCardPaymentInstruction> findFirstByPaymentIdAndStatusIn(
            UUID paymentId,
            Collection<ManualPaymentInstructionStatus> statuses
    );

    Optional<ManualCardPaymentInstruction> findFirstByCurrencyAndPayableAmountAndStatusIn(
            String currency,
            long payableAmount,
            Collection<ManualPaymentInstructionStatus> statuses
    );

    List<ManualCardPaymentInstruction> findAllByPaymentIdOrderByCreatedAtDesc(UUID paymentId);

    boolean existsByPaymentIdAndStatusIn(UUID paymentId, Collection<ManualPaymentInstructionStatus> statuses);
}
