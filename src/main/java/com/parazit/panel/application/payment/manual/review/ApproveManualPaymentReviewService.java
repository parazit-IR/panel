package com.parazit.panel.application.payment.manual.review;

import com.parazit.panel.application.payment.ApprovePaymentCommand;
import com.parazit.panel.application.payment.PaymentApprovalService;
import com.parazit.panel.application.payment.PaymentApprovalSource;
import com.parazit.panel.application.payment.manual.review.command.ApproveManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import com.parazit.panel.application.port.in.payment.manual.review.ApproveManualPaymentReviewUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.security.CurrentOperatorProvider;
import com.parazit.panel.config.properties.ManualPaymentReviewProperties;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReviewStatus;
import com.parazit.panel.domain.payment.manual.review.repository.ManualPaymentReviewRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveManualPaymentReviewService implements ApproveManualPaymentReviewUseCase {

    private final ManualPaymentReceiptRepository receiptRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final ManualPaymentReviewRepository reviewRepository;
    private final CurrentOperatorProvider operatorProvider;
    private final SystemClockPort clock;
    private final ManualPaymentReviewProperties properties;
    private final ManualPaymentReviewSupport support;
    private final ManualPaymentReviewResultMapper mapper;
    private final PaymentApprovalService paymentApprovalService;

    public ApproveManualPaymentReviewService(
            ManualPaymentReceiptRepository receiptRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            ManualPaymentReviewRepository reviewRepository,
            CurrentOperatorProvider operatorProvider,
            SystemClockPort clock,
            ManualPaymentReviewProperties properties,
            ManualPaymentReviewSupport support,
            ManualPaymentReviewResultMapper mapper,
            PaymentApprovalService paymentApprovalService
    ) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.instructionRepository = Objects.requireNonNull(instructionRepository, "instructionRepository must not be null");
        this.reviewRepository = Objects.requireNonNull(reviewRepository, "reviewRepository must not be null");
        this.operatorProvider = Objects.requireNonNull(operatorProvider, "operatorProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.support = Objects.requireNonNull(support, "support must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.paymentApprovalService = Objects.requireNonNull(paymentApprovalService, "paymentApprovalService must not be null");
    }

    @Override
    @Transactional
    public ManualPaymentReviewResult approve(ApproveManualPaymentReviewCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String operatorId = operatorProvider.currentOperatorId();
        Instant now = clock.now();
        ManualPaymentReceipt receipt = receiptRepository.findById(command.receiptId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(command.receiptId()));
        ManualPaymentReviewSupport.ManualReviewContext context = support.loadQueuedContext(receipt);
        ManualPaymentReview review = reviewRepository.findByReceiptIdForUpdate(receipt.getId())
                .orElseThrow(() -> new ManualPaymentReviewNotFoundException(receipt.getId()));
        if (review.getStatus() == ManualPaymentReviewStatus.APPROVED) {
            paymentApprovalService.approve(new ApprovePaymentCommand(
                    context.payment().getId(),
                    PaymentApprovalSource.MANUAL_OPERATOR_REVIEW,
                    review.getId().toString(),
                    null,
                    context.payment().getApprovedAt() == null ? now : context.payment().getApprovedAt()
            ));
            return mapper.toResult(review, context.payment(), context.order(), receipt, context.instruction(), false);
        }
        support.validateReviewable(context);
        validateApprovalPolicy(command, receipt);
        validateClaim(review, operatorId, now);

        review.approve(operatorId, command.operatorNote(), now);
        receipt.markApproved();
        context.instruction().markCompleted(now);
        paymentApprovalService.approve(new ApprovePaymentCommand(
                context.payment().getId(),
                PaymentApprovalSource.MANUAL_OPERATOR_REVIEW,
                review.getId().toString(),
                null,
                now
        ));
        receiptRepository.save(receipt);
        instructionRepository.save(context.instruction());
        review = reviewRepository.save(review);
        return mapper.toResult(review, context.payment(), context.order(), receipt, context.instruction(), true);
    }

    private void validateApprovalPolicy(ApproveManualPaymentReviewCommand command, ManualPaymentReceipt receipt) {
        if (properties.requireTrackingNumber() && receipt.getClaimedTrackingNumber() == null) {
            throw new ManualPaymentReviewNotAllowedException("Tracking number is required before approval");
        }
        if (properties.requireSenderCardLastFour() && receipt.getClaimedSenderCardLastFour() == null) {
            throw new ManualPaymentReviewNotAllowedException("Sender card last four digits are required before approval");
        }
        if (properties.requireOperatorNoteOnApproval()
                && (command.operatorNote() == null || command.operatorNote().isBlank())) {
            throw new ManualPaymentReviewNotAllowedException("Operator note is required before approval");
        }
    }

    private void validateClaim(ManualPaymentReview review, String operatorId, Instant now) {
        if (review.isClaimExpired(now, properties.claimTtl())) {
            throw new ManualPaymentReviewConflictException("Review claim has expired");
        }
        if (review.getStatus() != ManualPaymentReviewStatus.CLAIMED) {
            throw new ManualPaymentReviewConflictException("Review must be claimed before approval");
        }
        if (!operatorId.equals(review.getReviewerId())) {
            throw new ManualPaymentReviewConflictException("Review is claimed by another operator");
        }
    }
}
