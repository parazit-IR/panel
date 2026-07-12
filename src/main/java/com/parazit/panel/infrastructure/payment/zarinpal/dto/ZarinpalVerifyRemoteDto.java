package com.parazit.panel.infrastructure.payment.zarinpal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZarinpalVerifyRemoteDto(
        @JsonProperty("merchant_id") String merchantId,
        long amount,
        String authority
) {
}
