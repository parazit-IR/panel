package com.parazit.panel.api.internal.payment.manual.review;

import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentRejectionReason;
import com.parazit.panel.domain.payment.manual.review.ManualPaymentReviewStatus;
import java.time.Instant;
import java.util.UUID;

public record ManualPaymentReviewResponse(
        UUID reviewId,
        UUID receiptId,
        UUID paymentId,
        UUID orderId,
        ManualPaymentReviewStatus status,
        String reviewerId,
        Instant claimedAt,
        Instant decidedAt,
        ManualPaymentRejectionReason rejectionReason,
        String operatorNote,
        long expectedAmount,
        long claimedAmount,
        boolean amountMatched,
        boolean duplicateHashDetected,
        PaymentStatus paymentStatus,
        OrderStatus orderStatus,
        ManualPaymentReceiptStatus receiptStatus,
        ManualPaymentInstructionStatus instructionStatus,
        boolean changed
) {
}
