package com.parazit.panel.application.payment.manual.review.command;

import java.util.UUID;

public record ApproveManualPaymentReviewCommand(
        UUID receiptId,
        String operatorNote
) {

    public ApproveManualPaymentReviewCommand {
        java.util.Objects.requireNonNull(receiptId, "receiptId must not be null");
        operatorNote = operatorNote == null ? null : operatorNote.trim();
    }
}
