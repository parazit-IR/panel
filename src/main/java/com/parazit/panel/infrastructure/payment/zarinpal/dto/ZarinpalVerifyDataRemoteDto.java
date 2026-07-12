package com.parazit.panel.infrastructure.payment.zarinpal.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZarinpalVerifyDataRemoteDto(
        Integer code,
        String message,
        @JsonProperty("ref_id") Object refId,
        @JsonProperty("card_hash") String cardHash,
        @JsonProperty("card_pan") String cardPan,
        @JsonProperty("fee_type") String feeType,
        Long fee
) {
}
