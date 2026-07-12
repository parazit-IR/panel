package com.parazit.panel.api.internal.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.query.GetManualPaymentReviewQuery;
import com.parazit.panel.application.payment.manual.review.query.ListManualPaymentReviewsQuery;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import org.springframework.stereotype.Component;

@Component
public class ManualPaymentReviewApiMapper {

    public GetManualPaymentReviewQuery toGetQuery(java.util.UUID receiptId) {
        return new GetManualPaymentReviewQuery(receiptId);
    }

    public ListManualPaymentReviewsQuery toListQuery(Integer limit, Integer offset) {
        return new ListManualPaymentReviewsQuery(limit == null ? 50 : limit, offset == null ? 0 : offset);
    }

    public ManualPaymentReviewResponse toResponse(ManualPaymentReviewResult result) {
        return new ManualPaymentReviewResponse(
                result.reviewId(),
                result.receiptId(),
                result.paymentId(),
                result.orderId(),
                result.status(),
                result.reviewerId(),
                result.claimedAt(),
                result.decidedAt(),
                result.rejectionReason(),
                result.operatorNote(),
                result.expectedAmount(),
                result.claimedAmount(),
                result.amountMatched(),
                result.duplicateHashDetected(),
                result.paymentStatus(),
                result.orderStatus(),
                result.receiptStatus(),
                result.instructionStatus(),
                result.changed()
        );
    }
}
