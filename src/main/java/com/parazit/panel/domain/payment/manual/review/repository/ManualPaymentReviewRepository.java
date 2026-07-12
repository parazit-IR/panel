package com.parazit.panel.domain.payment.manual.review.repository;

import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.repository.UuidRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManualPaymentReviewRepository extends UuidRepository<ManualPaymentReview> {

    Optional<ManualPaymentReview> findByReceiptId(UUID receiptId);

    Optional<ManualPaymentReview> findClaimedByReceiptId(UUID receiptId);

    List<ManualPaymentReview> findAllByReviewerIdOrderByClaimedAtDesc(String reviewerId);

    List<ManualPaymentReview> findAllByStatusOrderByClaimedAtDesc(com.parazit.panel.domain.payment.manual.review.ManualPaymentReviewStatus status);

    List<ManualPaymentReview> findAllOrderByCreatedAtDesc(int limit);

    boolean existsByReceiptId(UUID receiptId);
}
