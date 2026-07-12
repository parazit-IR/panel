package com.parazit.panel.infrastructure.payment.zarinpal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ZarinpalRequestRemoteDto(
        @JsonProperty("merchant_id") String merchantId,
        long amount,
        String currency,
        @JsonProperty("callback_url") String callbackUrl,
        String description,
        Map<String, String> metadata
) {
}
