package com.parazit.panel.api.payment.manual;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record InitializeManualCardPaymentRequest(
        @NotNull UUID instructionRequestId,
        @NotNull @Positive Long telegramUserId
) {
}
