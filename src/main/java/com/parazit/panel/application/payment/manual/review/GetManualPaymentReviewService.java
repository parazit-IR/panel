package com.parazit.panel.application.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.query.GetManualPaymentReviewQuery;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import com.parazit.panel.application.port.in.payment.manual.review.GetManualPaymentReviewUseCase;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.payment.manual.review.repository.ManualPaymentReviewRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetManualPaymentReviewService implements GetManualPaymentReviewUseCase {

    private final ManualPaymentReceiptRepository receiptRepository;
    private final ManualPaymentReviewRepository reviewRepository;
    private final ManualPaymentReviewSupport support;
    private final ManualPaymentReviewResultMapper mapper;

    public GetManualPaymentReviewService(
            ManualPaymentReceiptRepository receiptRepository,
            ManualPaymentReviewRepository reviewRepository,
            ManualPaymentReviewSupport support,
            ManualPaymentReviewResultMapper mapper
    ) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "reviewRepository must not be null");
        this.support = Objects.requireNonNull(support, "support must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public ManualPaymentReviewResult get(GetManualPaymentReviewQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        ManualPaymentReceipt receipt = receiptRepository.findById(query.receiptId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(query.receiptId()));
        ManualPaymentReview review = reviewRepository.findByReceiptId(receipt.getId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(receipt.getId()));
        ManualPaymentReviewSupport.ManualReviewContext context = support.loadQueuedContext(receipt);
        return mapper.toResult(review, context.payment(), context.order(), receipt, context.instruction(), false);
    }
}
