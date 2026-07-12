package com.parazit.panel.application.payment.zarinpal.model;

public record ZarinpalVerifyResponse(
        boolean successful,
        boolean alreadyVerified,
        int code,
        String message,
        String referenceId,
        String cardHash,
        String cardPanMasked,
        long fee,
        String feeType
) {
}
