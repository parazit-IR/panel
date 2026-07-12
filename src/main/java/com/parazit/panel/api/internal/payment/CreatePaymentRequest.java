package com.parazit.panel.api.internal.payment;

import com.parazit.panel.domain.payment.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID orderId,
        @NotNull UUID userId,
        @NotNull PaymentMethod paymentMethod,
        @PositiveOrZero long amount,
        @NotBlank String currency
) {
}
