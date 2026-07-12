package com.parazit.panel.infrastructure.persistence.payment.manual;

import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ManualCardPaymentInstructionRepositoryAdapter
        extends JpaRepositoryAdapter<ManualCardPaymentInstruction, UUID>
        implements ManualCardPaymentInstructionRepository {

    private static final List<ManualPaymentInstructionStatus> ACTIVE_STATUSES = List.of(
            ManualPaymentInstructionStatus.CREATED,
            ManualPaymentInstructionStatus.ACTIVE,
            ManualPaymentInstructionStatus.RECEIPT_PENDING
    );

    private final SpringDataManualCardPaymentInstructionRepository repository;

    public ManualCardPaymentInstructionRepositoryAdapter(
            SpringDataManualCardPaymentInstructionRepository repository
    ) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public ManualCardPaymentInstruction save(ManualCardPaymentInstruction instruction) {
        return repository.saveAndFlush(Objects.requireNonNull(instruction, "instruction must not be null"));
    }

    @Override
    public Optional<ManualCardPaymentInstruction> findByInstructionRequestId(UUID instructionRequestId) {
        return repository.findByInstructionRequestId(
                Objects.requireNonNull(instructionRequestId, "instructionRequestId must not be null")
        );
    }

    @Override
    public Optional<ManualCardPaymentInstruction> findActiveByPaymentId(UUID paymentId) {
        return repository.findFirstByPaymentIdAndStatusIn(
                Objects.requireNonNull(paymentId, "paymentId must not be null"),
                ACTIVE_STATUSES
        );
    }

    @Override
    public Optional<ManualCardPaymentInstruction> findActiveByCurrencyAndPayableAmount(
            String currency,
            long payableAmount
    ) {
        return repository.findFirstByCurrencyAndPayableAmountAndStatusIn(
                Objects.requireNonNull(currency, "currency must not be null").trim().toUpperCase(),
                payableAmount,
                ACTIVE_STATUSES
        );
    }

    @Override
    public List<ManualCardPaymentInstruction> findAllByPaymentIdOrderByCreatedAtDesc(UUID paymentId) {
        return repository.findAllByPaymentIdOrderByCreatedAtDesc(
                Objects.requireNonNull(paymentId, "paymentId must not be null")
        );
    }

    @Override
    public boolean existsActiveByPaymentId(UUID paymentId) {
        return repository.existsByPaymentIdAndStatusIn(
                Objects.requireNonNull(paymentId, "paymentId must not be null"),
                ACTIVE_STATUSES
        );
    }
}
