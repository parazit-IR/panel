package com.parazit.panel.application.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.query.ListManualPaymentReviewsQuery;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import com.parazit.panel.application.port.in.payment.manual.review.ListManualPaymentReviewsUseCase;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.payment.manual.review.repository.ManualPaymentReviewRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListManualPaymentReviewsService implements ListManualPaymentReviewsUseCase {

    private final ManualPaymentReviewRepository reviewRepository;
    private final ManualPaymentReceiptRepository receiptRepository;
    private final ManualPaymentReviewSupport support;
    private final ManualPaymentReviewResultMapper mapper;

    public ListManualPaymentReviewsService(
            ManualPaymentReviewRepository reviewRepository,
            ManualPaymentReceiptRepository receiptRepository,
            ManualPaymentReviewSupport support,
            ManualPaymentReviewResultMapper mapper
    ) {
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "reviewRepository must not be null");
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.support = Objects.requireNonNull(support, "support must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualPaymentReviewResult> list(ListManualPaymentReviewsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        int limit = Math.max(1, Math.min(query.limit() <= 0 ? 50 : query.limit(), 200));
        return reviewRepository.findAllOrderByCreatedAtDesc(limit)
                .stream()
                .map(this::toResult)
                .toList();
    }

    private ManualPaymentReviewResult toResult(ManualPaymentReview review) {
        ManualPaymentReceipt receipt = receiptRepository.findById(review.getReceiptId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(review.getReceiptId()));
        ManualPaymentReviewSupport.ManualReviewContext context = support.loadQueuedContext(receipt);
        return mapper.toResult(review, context.payment(), context.order(), context.receipt(), context.instruction(), false);
    }
}
