package com.parazit.panel.application.payment.command;

import com.parazit.panel.domain.payment.PaymentMethod;
import java.util.UUID;

public record CreatePaymentCommand(
        UUID orderId,
        UUID userId,
        PaymentMethod paymentMethod,
        long amount,
        String currency
) {
}
