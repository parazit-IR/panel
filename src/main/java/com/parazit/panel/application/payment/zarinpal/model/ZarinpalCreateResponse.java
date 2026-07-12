package com.parazit.panel.application.payment.zarinpal.model;

public record ZarinpalCreateResponse(
        boolean successful,
        String authority,
        int code,
        String message,
        String paymentUrl
) {
}
