package com.parazit.panel.application.payment.zarinpal.model;

public record ZarinpalVerifyRequest(
        String merchantId,
        long amount,
        String authority
) {
}
