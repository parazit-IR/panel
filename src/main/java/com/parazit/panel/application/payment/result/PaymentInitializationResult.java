package com.parazit.panel.application.payment.result;

import com.parazit.panel.domain.payment.PaymentMethod;
import java.util.UUID;

public record PaymentInitializationResult(
        UUID paymentId,
        PaymentMethod paymentMethod,
        boolean initialized,
        String gatewayAuthority,
        String redirectUrl,
        String safeMessage
) {
}
