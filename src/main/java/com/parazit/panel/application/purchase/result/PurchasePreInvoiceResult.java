package com.parazit.panel.application.purchase.result;

import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Instant;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

public record PurchasePreInvoiceResult(
        UUID purchaseSessionId,
        UUID planSelectionId,
        String customerDisplayName,
        String serviceName,
        String planName,
        String planDescription,
        int durationDays,
        OptionalLong trafficBytes,
        OptionalInt maxDevices,
        long originalAmount,
        long discountAmount,
        long finalAmount,
        CurrencyCode currency,
        boolean discountFeatureAvailable,
        boolean walletFeatureAvailable,
        boolean manualPaymentAvailable,
        boolean onlinePaymentAvailable,
        Instant selectionExpiresAt,
        Instant generatedAt
) {
}
