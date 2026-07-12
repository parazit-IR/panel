package com.parazit.panel.application.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.command.ClaimManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import com.parazit.panel.application.port.in.payment.manual.review.ClaimManualPaymentReviewUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.security.CurrentOperatorProvider;
import com.parazit.panel.config.properties.ManualPaymentReviewProperties;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.payment.manual.review.repository.ManualPaymentReviewRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClaimManualPaymentReviewService implements ClaimManualPaymentReviewUseCase {

    private final ManualPaymentReceiptRepository receiptRepository;
    private final ManualPaymentReviewRepository reviewRepository;
    private final CurrentOperatorProvider operatorProvider;
    private final SystemClockPort clock;
    private final ManualPaymentReviewProperties properties;
    private final ManualPaymentReviewSupport support;
    private final ManualPaymentReviewResultMapper mapper;

    public ClaimManualPaymentReviewService(
            ManualPaymentReceiptRepository receiptRepository,
            ManualPaymentReviewRepository reviewRepository,
            CurrentOperatorProvider operatorProvider,
            SystemClockPort clock,
            ManualPaymentReviewProperties properties,
            ManualPaymentReviewSupport support,
            ManualPaymentReviewResultMapper mapper
    ) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "reviewRepository must not be null");
        this.operatorProvider = Objects.requireNonNull(operatorProvider, "operatorProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.support = Objects.requireNonNull(support, "support must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public ManualPaymentReviewResult claim(ClaimManualPaymentReviewCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String operatorId = operatorProvider.currentOperatorId();
        Instant now = clock.now();
        ManualPaymentReceipt receipt = receiptRepository.findById(command.receiptId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(command.receiptId()));
        ManualPaymentReviewSupport.ManualReviewContext context = support.loadQueuedContext(receipt);
        support.validateReviewable(context);
        ManualPaymentReview review = reviewRepository.findByReceiptId(receipt.getId())
                .orElseGet(() -> ManualPaymentReview.create(
                        receipt.getId(),
                        receipt.getPaymentId(),
                        context.payment().getOrderId(),
                        context.instruction().getPayableAmount(),
                        receipt.getClaimedAmount(),
                        receipt.isDuplicateHashDetected()
                ));
        boolean changed = false;
        if (review.getStatus() == com.parazit.panel.domain.payment.manual.review.ManualPaymentReviewStatus.CLAIMED
                && operatorId.equals(review.getReviewerId())) {
            return mapper.toResult(review, context.payment(), context.order(), receipt, context.instruction(), false);
        }
        if (review.getStatus() == com.parazit.panel.domain.payment.manual.review.ManualPaymentReviewStatus.CLAIMED
                && !review.isClaimExpired(now, properties.claimTtl())) {
            throw new ManualPaymentReviewConflictException("Receipt review is claimed by another operator");
        }
        try {
            review.claim(operatorId, now);
            changed = true;
            review = reviewRepository.save(review);
        } catch (DataIntegrityViolationException exception) {
            throw new ManualPaymentReviewConflictException("Receipt review was claimed concurrently");
        }
        return mapper.toResult(review, context.payment(), context.order(), receipt, context.instruction(), changed);
    }
}
