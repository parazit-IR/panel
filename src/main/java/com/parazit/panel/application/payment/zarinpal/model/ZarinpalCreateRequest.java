package com.parazit.panel.application.payment.zarinpal.model;

public record ZarinpalCreateRequest(
        String merchantId,
        long amount,
        String currency,
        String callbackUrl,
        String description,
        String mobile,
        String email
) {
}
