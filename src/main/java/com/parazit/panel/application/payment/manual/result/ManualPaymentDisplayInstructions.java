package com.parazit.panel.application.payment.manual.result;

import java.time.Instant;
import java.util.List;

public record ManualPaymentDisplayInstructions(
        String title,
        long baseAmount,
        long suffixAmount,
        long payableAmount,
        String currency,
        String bankName,
        String cardHolderName,
        String formattedCardNumber,
        Instant expiresAt,
        List<String> warnings
) {
}
