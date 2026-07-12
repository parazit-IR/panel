package com.parazit.panel.infrastructure.payment.zarinpal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZarinpalRequestDataRemoteDto(
        Integer code,
        String message,
        String authority,
        @JsonProperty("fee_type") String feeType,
        Long fee
) {
}
