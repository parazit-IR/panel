package com.parazit.panel.application.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReview;
import org.springframework.stereotype.Component;

@Component
public class ManualPaymentReviewResultMapper {

    public ManualPaymentReviewResult toResult(
            ManualPaymentReview review,
            Payment payment,
            Order order,
            ManualPaymentReceipt receipt,
            ManualCardPaymentInstruction instruction,
            boolean changed
    ) {
        return new ManualPaymentReviewResult(
                review.getId(),
                review.getReceiptId(),
                review.getPaymentId(),
                review.getOrderId(),
                review.getStatus(),
                review.getReviewerId(),
                review.getClaimedAt(),
                review.getDecidedAt(),
                review.getDecisionReason(),
                review.getOperatorNote(),
                review.getExpectedAmount(),
                review.getClaimedAmount(),
                review.isAmountMatched(),
                review.isDuplicateHashDetected(),
                payment.getStatus(),
                order.getStatus(),
                receipt.getStatus(),
                instruction.getStatus(),
                changed
        );
    }
}
