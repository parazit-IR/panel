package com.parazit.panel.infrastructure.payment.zarinpal.dto;

public record ZarinpalErrorRemoteDto(
        Integer code,
        String message,
        String validations
) {
}
