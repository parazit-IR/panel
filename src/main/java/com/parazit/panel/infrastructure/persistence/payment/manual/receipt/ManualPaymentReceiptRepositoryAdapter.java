package com.parazit.panel.infrastructure.persistence.payment.manual.receipt;

import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class ManualPaymentReceiptRepositoryAdapter
        extends JpaRepositoryAdapter<ManualPaymentReceipt, UUID>
        implements ManualPaymentReceiptRepository {

    private static final List<ManualPaymentReceiptStatus> ACTIVE_STATUSES = List.of(
            ManualPaymentReceiptStatus.UPLOADING,
            ManualPaymentReceiptStatus.SUBMITTED,
            ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW
    );

    private final SpringDataManualPaymentReceiptRepository repository;

    public ManualPaymentReceiptRepositoryAdapter(SpringDataManualPaymentReceiptRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public ManualPaymentReceipt save(ManualPaymentReceipt receipt) {
        return repository.saveAndFlush(Objects.requireNonNull(receipt, "receipt must not be null"));
    }

    @Override
    public Optional<ManualPaymentReceipt> findByReceiptRequestId(UUID receiptRequestId) {
        return repository.findByReceiptRequestId(
                Objects.requireNonNull(receiptRequestId, "receiptRequestId must not be null")
        );
    }

    @Override
    public Optional<ManualPaymentReceipt> findActiveByInstructionId(UUID instructionId) {
        return repository.findFirstByInstructionIdAndStatusIn(
                Objects.requireNonNull(instructionId, "instructionId must not be null"),
                ACTIVE_STATUSES
        );
    }

    @Override
    public Optional<ManualPaymentReceipt> findByStorageKey(String storageKey) {
        return repository.findByStorageKey(Objects.requireNonNull(storageKey, "storageKey must not be null"));
    }

    @Override
    public Optional<ManualPaymentReceipt> findActiveByUserIdAndFileSha256(UUID userId, String fileSha256) {
        return repository.findFirstByUserIdAndFileSha256AndStatusIn(
                Objects.requireNonNull(userId, "userId must not be null"),
                Objects.requireNonNull(fileSha256, "fileSha256 must not be null"),
                ACTIVE_STATUSES
        );
    }

    @Override
    public Optional<ManualPaymentReceipt> findActiveByFileSha256(String fileSha256) {
        return repository.findFirstByFileSha256AndStatusIn(
                Objects.requireNonNull(fileSha256, "fileSha256 must not be null"),
                ACTIVE_STATUSES
        );
    }

    @Override
    public List<ManualPaymentReceipt> findAllByPaymentIdOrderBySubmittedAtDesc(UUID paymentId) {
        return repository.findAllByPaymentIdOrderBySubmittedAtDesc(
                Objects.requireNonNull(paymentId, "paymentId must not be null")
        );
    }

    @Override
    public List<ManualPaymentReceipt> findAllQueuedForReviewOrderByReviewQueuedAtAsc(int limit, int offset) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        int safeOffset = Math.max(0, offset);
        return repository.findAllByStatusOrderByReviewQueuedAtAsc(
                ManualPaymentReceiptStatus.QUEUED_FOR_REVIEW,
                PageRequest.of(safeOffset / safeLimit, safeLimit)
        );
    }

    @Override
    public boolean existsActiveByInstructionId(UUID instructionId) {
        return repository.existsByInstructionIdAndStatusIn(
                Objects.requireNonNull(instructionId, "instructionId must not be null"),
                ACTIVE_STATUSES
        );
    }
}
