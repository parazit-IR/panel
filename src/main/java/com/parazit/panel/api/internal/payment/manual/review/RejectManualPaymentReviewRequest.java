package com.parazit.panel.api.internal.payment.manual.review;

import com.parazit.panel.domain.payment.manual.review.ManualPaymentRejectionReason;
import jakarta.validation.constraints.NotNull;

public record RejectManualPaymentReviewRequest(
        @NotNull ManualPaymentRejectionReason reason,
        String operatorNote
) {
}
