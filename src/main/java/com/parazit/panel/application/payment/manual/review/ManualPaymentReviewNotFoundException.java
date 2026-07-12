package com.parazit.panel.application.payment.manual.review;

import java.util.UUID;

public class ManualPaymentReviewNotFoundException extends RuntimeException {

    public ManualPaymentReviewNotFoundException(UUID receiptId) {
        super("Manual payment review was not found for receipt " + receiptId);
    }
}
