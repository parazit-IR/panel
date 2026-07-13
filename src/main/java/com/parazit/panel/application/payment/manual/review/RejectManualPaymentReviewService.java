package com.parazit.panel.application.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.command.RejectManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import com.parazit.panel.application.port.in.payment.manual.review.RejectManualPaymentReviewUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.security.CurrentOperatorProvider;
import com.parazit.panel.config.properties.ManualPaymentReviewProperties;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReviewStatus;
import com.parazit.panel.domain.payment.manual.review.repository.ManualPaymentReviewRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RejectManualPaymentReviewService implements RejectManualPaymentReviewUseCase {

    private final ManualPaymentReceiptRepository receiptRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final PaymentRepository paymentRepository;
    private final ManualPaymentReviewRepository reviewRepository;
    private final CurrentOperatorProvider operatorProvider;
    private final SystemClockPort clock;
    private final ManualPaymentReviewProperties properties;
    private final ManualPaymentReviewSupport support;
    private final ManualPaymentReviewResultMapper mapper;

    public RejectManualPaymentReviewService(
            ManualPaymentReceiptRepository receiptRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            PaymentRepository paymentRepository,
            ManualPaymentReviewRepository reviewRepository,
            CurrentOperatorProvider operatorProvider,
            SystemClockPort clock,
            ManualPaymentReviewProperties properties,
            ManualPaymentReviewSupport support,
            ManualPaymentReviewResultMapper mapper
    ) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.instructionRepository = Objects.requireNonNull(instructionRepository, "instructionRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "reviewRepository must not be null");
        this.operatorProvider = Objects.requireNonNull(operatorProvider, "operatorProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.support = Objects.requireNonNull(support, "support must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public ManualPaymentReviewResult reject(RejectManualPaymentReviewCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String operatorId = operatorProvider.currentOperatorId();
        Instant now = clock.now();
        ManualPaymentReceipt receipt = receiptRepository.findById(command.receiptId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(command.receiptId()));
        ManualPaymentReviewSupport.ManualReviewContext context = support.loadQueuedContext(receipt);
        support.validateReviewable(context);
        ManualPaymentReview review = reviewRepository.findByReceiptIdForUpdate(receipt.getId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(receipt.getId()));
        validateClaim(review, operatorId, now);

        review.reject(operatorId, command.reason(), command.operatorNote(), now);
        receipt.markRejected();
        context.instruction().returnToActiveAfterReceiptRejection(now);
        context.payment().returnToWaitingForPaymentAfterReviewRejection();
        receiptRepository.save(receipt);
        instructionRepository.save(context.instruction());
        paymentRepository.save(context.payment());
        review = reviewRepository.save(review);
        return mapper.toResult(review, context.payment(), context.order(), receipt, context.instruction(), true);
    }

    private void validateClaim(ManualPaymentReview review, String operatorId, Instant now) {
        if (review.isClaimExpired(now, properties.claimTtl())) {
            throw new ManualPaymentReviewConflictException("Review claim has expired");
        }
        if (review.getStatus() != ManualPaymentReviewStatus.CLAIMED) {
            throw new ManualPaymentReviewConflictException("Review must be claimed before rejection");
        }
        if (!operatorId.equals(review.getReviewerId())) {
            throw new ManualPaymentReviewConflictException("Review is claimed by another operator");
        }
    }
}
