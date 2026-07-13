package com.parazit.panel.application.purchase.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchasePaymentMethodsResult(
        UUID purchaseSessionId,
        UUID orderId,
        long finalAmount,
        String currency,
        List<AvailablePaymentMethodResult> methods,
        Instant generatedAt
) {
}
