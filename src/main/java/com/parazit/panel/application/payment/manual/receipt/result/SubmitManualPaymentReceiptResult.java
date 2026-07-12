package com.parazit.panel.application.payment.manual.receipt.result;

public record SubmitManualPaymentReceiptResult(
        ManualPaymentReceiptResult receipt,
        boolean newlySubmitted
) {
}
