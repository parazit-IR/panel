package com.parazit.panel.application.renewal.result;

import com.parazit.panel.application.purchase.result.AvailablePaymentMethodResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateRenewalOrderResult(
        UUID purchaseSessionId,
        UUID orderId,
        long renewalAmount,
        String currency,
        List<AvailablePaymentMethodResult> methods,
        Instant createdAt,
        boolean reused
) {
}
