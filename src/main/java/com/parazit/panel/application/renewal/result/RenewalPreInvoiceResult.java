package com.parazit.panel.application.renewal.result;

import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.RenewalExpiryPolicy;
import com.parazit.panel.domain.order.RenewalTrafficPolicy;
import com.parazit.panel.domain.plan.CurrencyCode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public record RenewalPreInvoiceResult(
        UUID purchaseSessionId,
        UUID renewalSelectionId,
        UUID targetSubscriptionId,
        String customerDisplayName,
        String serviceDisplayName,
        String serviceUsername,
        CustomerServiceStatus currentStatus,
        Optional<Instant> currentExpiryAt,
        OptionalLong currentTrafficLimitBytes,
        OptionalLong currentRemainingTrafficBytes,
        String renewalPlanName,
        String renewalPlanDescription,
        Duration renewalDuration,
        OptionalLong renewalTrafficBytes,
        RenewalTrafficPolicy trafficPolicy,
        RenewalExpiryPolicy expiryPolicy,
        Instant proposedExpiryAt,
        Money originalAmount,
        Money finalAmount,
        CurrencyCode currency,
        Instant selectionExpiresAt,
        boolean manualPaymentAvailable,
        boolean onlinePaymentAvailable
) {
}
