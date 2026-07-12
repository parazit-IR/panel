package com.parazit.panel.application.payment.manual.receipt.query;

public record ListManualPaymentReviewQueueQuery(int limit, int offset, boolean duplicateOnly) {
}
