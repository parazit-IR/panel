package com.parazit.panel.application.payment.command;

import com.parazit.panel.domain.payment.PaymentMethod;
import java.util.UUID;

public record PaymentVerificationCommand(
        UUID paymentId,
        PaymentMethod paymentMethod,
        String authority,
        String transactionId
) {
}
