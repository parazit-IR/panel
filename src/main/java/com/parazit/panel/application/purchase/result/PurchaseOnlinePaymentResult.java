package com.parazit.panel.application.purchase.result;

import com.parazit.panel.application.payment.zarinpal.result.InitializeZarinpalPaymentResult;
import java.util.UUID;

public record PurchaseOnlinePaymentResult(
        UUID purchaseSessionId,
        UUID orderId,
        long finalAmount,
        String currency,
        InitializeZarinpalPaymentResult payment
) {
}
