package com.parazit.panel.infrastructure.persistence.payment.manual.receipt;

import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface SpringDataManualPaymentReceiptRepository extends SpringDataUuidRepository<ManualPaymentReceipt> {

    Optional<ManualPaymentReceipt> findByReceiptRequestId(UUID receiptRequestId);

    Optional<ManualPaymentReceipt> findFirstByInstructionIdAndStatusIn(
            UUID instructionId,
            Collection<ManualPaymentReceiptStatus> statuses
    );

    Optional<ManualPaymentReceipt> findByStorageKey(String storageKey);

    Optional<ManualPaymentReceipt> findFirstByUserIdAndFileSha256AndStatusIn(
            UUID userId,
            String fileSha256,
            Collection<ManualPaymentReceiptStatus> statuses
    );

    Optional<ManualPaymentReceipt> findFirstByFileSha256AndStatusIn(
            String fileSha256,
            Collection<ManualPaymentReceiptStatus> statuses
    );

    List<ManualPaymentReceipt> findAllByPaymentIdOrderBySubmittedAtDesc(UUID paymentId);

    List<ManualPaymentReceipt> findAllByStatusOrderByReviewQueuedAtAsc(
            ManualPaymentReceiptStatus status,
            Pageable pageable
    );

    boolean existsByInstructionIdAndStatusIn(UUID instructionId, Collection<ManualPaymentReceiptStatus> statuses);
}
