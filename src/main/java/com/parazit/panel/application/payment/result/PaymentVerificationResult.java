package com.parazit.panel.application.payment.result;

import com.parazit.panel.domain.payment.PaymentMethod;
import java.util.UUID;

public record PaymentVerificationResult(
        UUID paymentId,
        PaymentMethod paymentMethod,
        boolean verified,
        String gatewayTransactionId,
        String safeMessage
) {
}
