package com.parazit.panel.application.payment.manual.review.query;

import java.util.Objects;
import java.util.UUID;

public record GetManualPaymentReviewQuery(UUID receiptId) {

    public GetManualPaymentReviewQuery {
        Objects.requireNonNull(receiptId, "receiptId must not be null");
    }
}
