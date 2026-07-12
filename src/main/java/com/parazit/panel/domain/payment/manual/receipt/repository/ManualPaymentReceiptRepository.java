package com.parazit.panel.domain.payment.manual.receipt.repository;

import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManualPaymentReceiptRepository extends UuidRepository<ManualPaymentReceipt> {

    Optional<ManualPaymentReceipt> findByReceiptRequestId(UUID receiptRequestId);

    Optional<ManualPaymentReceipt> findActiveByInstructionId(UUID instructionId);

    Optional<ManualPaymentReceipt> findByStorageKey(String storageKey);

    Optional<ManualPaymentReceipt> findActiveByUserIdAndFileSha256(UUID userId, String fileSha256);

    Optional<ManualPaymentReceipt> findActiveByFileSha256(String fileSha256);

    List<ManualPaymentReceipt> findAllByPaymentIdOrderBySubmittedAtDesc(UUID paymentId);

    List<ManualPaymentReceipt> findAllQueuedForReviewOrderByReviewQueuedAtAsc(int limit, int offset);

    boolean existsActiveByInstructionId(UUID instructionId);
}
