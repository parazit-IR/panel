package com.parazit.panel.application.payment.manual.review.command;

import com.parazit.panel.domain.payment.manual.review.ManualPaymentRejectionReason;
import java.util.Objects;
import java.util.UUID;

public record RejectManualPaymentReviewCommand(
        UUID receiptId,
        ManualPaymentRejectionReason reason,
        String operatorNote
) {

    public RejectManualPaymentReviewCommand {
        Objects.requireNonNull(receiptId, "receiptId must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        operatorNote = operatorNote == null ? null : operatorNote.trim();
    }
}
