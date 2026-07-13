package com.parazit.panel.application.purchase.result;

import com.parazit.panel.application.sales.PaymentMethodCapability;
import com.parazit.panel.domain.payment.PaymentMethod;

public record AvailablePaymentMethodResult(
        PaymentMethod method,
        boolean enabled,
        String displayLabelKey,
        int displayOrder,
        PaymentMethodCapability capability,
        String unavailableReasonCode
) {
}
