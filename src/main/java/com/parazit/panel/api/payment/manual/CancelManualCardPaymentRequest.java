package com.parazit.panel.api.payment.manual;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CancelManualCardPaymentRequest(
        @NotNull @Positive Long telegramUserId
) {
}
