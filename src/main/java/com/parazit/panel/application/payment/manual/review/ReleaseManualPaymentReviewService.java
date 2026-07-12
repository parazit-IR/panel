package com.parazit.panel.application.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.command.ReleaseManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import com.parazit.panel.application.port.in.payment.manual.review.ReleaseManualPaymentReviewUseCase;
import com.parazit.panel.application.port.out.security.CurrentOperatorProvider;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.payment.manual.review.repository.ManualPaymentReviewRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseManualPaymentReviewService implements ReleaseManualPaymentReviewUseCase {

    private final ManualPaymentReceiptRepository receiptRepository;
    private final ManualPaymentReviewRepository reviewRepository;
    private final CurrentOperatorProvider operatorProvider;
    private final ManualPaymentReviewSupport support;
    private final ManualPaymentReviewResultMapper mapper;

    public ReleaseManualPaymentReviewService(
            ManualPaymentReceiptRepository receiptRepository,
            ManualPaymentReviewRepository reviewRepository,
            CurrentOperatorProvider operatorProvider,
            ManualPaymentReviewSupport support,
            ManualPaymentReviewResultMapper mapper
    ) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "reviewRepository must not be null");
        this.operatorProvider = Objects.requireNonNull(operatorProvider, "operatorProvider must not be null");
        this.support = Objects.requireNonNull(support, "support must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public ManualPaymentReviewResult release(ReleaseManualPaymentReviewCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ManualPaymentReceipt receipt = receiptRepository.findById(command.receiptId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(command.receiptId()));
        ManualPaymentReviewSupport.ManualReviewContext context = support.loadQueuedContext(receipt);
        ManualPaymentReview review = reviewRepository.findByReceiptId(receipt.getId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(receipt.getId()));
        review.release(operatorProvider.currentOperatorId());
        review = reviewRepository.save(review);
        return mapper.toResult(review, context.payment(), context.order(), receipt, context.instruction(), true);
    }
}
