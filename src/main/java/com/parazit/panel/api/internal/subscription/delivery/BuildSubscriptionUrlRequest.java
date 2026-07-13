package com.parazit.panel.api.internal.subscription.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildSubscriptionUrlRequest(
        @NotBlank
        @Size(max = 512)
        String accessToken
) {
}

