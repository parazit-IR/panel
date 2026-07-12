package com.parazit.panel.api.payment.zarinpal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record InitializeZarinpalPaymentRequest(
        @NotNull UUID requestId,
        @NotNull @Positive Long telegramUserId,
        @NotBlank @Size(max = 500) String description,
        @Size(max = 32) String mobile,
        @Email @Size(max = 255) String email
) {
}
