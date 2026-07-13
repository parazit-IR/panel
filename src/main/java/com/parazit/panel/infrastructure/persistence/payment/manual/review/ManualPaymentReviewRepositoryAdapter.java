package com.parazit.panel.infrastructure.persistence.payment.manual.review;

import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReviewStatus;
import com.parazit.panel.domain.payment.manual.review.repository.ManualPaymentReviewRepository;
import com.parazit.panel.infrastructure.persistence.repository.JpaRepositoryAdapter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ManualPaymentReviewRepositoryAdapter
        extends JpaRepositoryAdapter<ManualPaymentReview, UUID>
        implements ManualPaymentReviewRepository {

    private final SpringDataManualPaymentReviewRepository repository;

    public ManualPaymentReviewRepositoryAdapter(SpringDataManualPaymentReviewRepository repository) {
        super(repository);
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public ManualPaymentReview save(ManualPaymentReview review) {
        return repository.saveAndFlush(Objects.requireNonNull(review, "review must not be null"));
    }

    @Override
    public Optional<ManualPaymentReview> findByReceiptId(UUID receiptId) {
        return repository.findByReceiptId(Objects.requireNonNull(receiptId, "receiptId must not be null"));
    }

    @Override
    public Optional<ManualPaymentReview> findByReceiptIdForUpdate(UUID receiptId) {
        return repository.findWithLockByReceiptId(Objects.requireNonNull(receiptId, "receiptId must not be null"));
    }

    @Override
    public Optional<ManualPaymentReview> findClaimedByReceiptId(UUID receiptId) {
        return repository.findByReceiptIdAndStatus(
                Objects.requireNonNull(receiptId, "receiptId must not be null"),
                ManualPaymentReviewStatus.CLAIMED
        );
    }

    @Override
    public List<ManualPaymentReview> findAllByReviewerIdOrderByClaimedAtDesc(String reviewerId) {
        return repository.findAllByReviewerIdOrderByClaimedAtDesc(
                Objects.requireNonNull(reviewerId, "reviewerId must not be null")
        );
    }

    @Override
    public List<ManualPaymentReview> findAllByStatusOrderByClaimedAtDesc(ManualPaymentReviewStatus status) {
        return repository.findAllByStatusOrderByClaimedAtDesc(
                Objects.requireNonNull(status, "status must not be null")
        );
    }

    @Override
    public List<ManualPaymentReview> findAllOrderByCreatedAtDesc(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return repository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, safeLimit));
    }

    @Override
    public boolean existsByReceiptId(UUID receiptId) {
        return repository.existsByReceiptId(Objects.requireNonNull(receiptId, "receiptId must not be null"));
    }
}
