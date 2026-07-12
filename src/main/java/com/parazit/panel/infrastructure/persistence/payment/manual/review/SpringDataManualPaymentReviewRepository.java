package com.parazit.panel.infrastructure.persistence.payment.manual.review;

import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReviewStatus;
import com.parazit.panel.infrastructure.persistence.repository.SpringDataUuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataManualPaymentReviewRepository extends SpringDataUuidRepository<ManualPaymentReview> {

    Optional<ManualPaymentReview> findByReceiptId(UUID receiptId);

    Optional<ManualPaymentReview> findByReceiptIdAndStatus(UUID receiptId, ManualPaymentReviewStatus status);

    List<ManualPaymentReview> findAllByReviewerIdOrderByClaimedAtDesc(String reviewerId);

    List<ManualPaymentReview> findAllByStatusOrderByClaimedAtDesc(ManualPaymentReviewStatus status);

    List<ManualPaymentReview> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    boolean existsByReceiptId(UUID receiptId);
}
