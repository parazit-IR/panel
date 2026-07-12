package com.parazit.panel.application.payment.manual.review.command;

import java.util.Objects;
import java.util.UUID;

public record ReleaseManualPaymentReviewCommand(UUID receiptId) {

    public ReleaseManualPaymentReviewCommand {
        Objects.requireNonNull(receiptId, "receiptId must not be null");
    }
}
