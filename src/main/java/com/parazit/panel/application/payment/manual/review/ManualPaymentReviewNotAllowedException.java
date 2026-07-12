package com.parazit.panel.application.payment.manual.review;

public class ManualPaymentReviewNotAllowedException extends RuntimeException {

    public ManualPaymentReviewNotAllowedException(String message) {
        super(message);
    }
}
