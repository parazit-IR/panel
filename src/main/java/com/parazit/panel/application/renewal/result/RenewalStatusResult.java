package com.parazit.panel.application.renewal.result;

import com.parazit.panel.domain.order.OrderStatus;
import com.parazit.panel.domain.order.OrderType;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.renewal.RenewalOutboxStatus;
import java.time.Instant;
import java.util.UUID;

public record RenewalStatusResult(
        UUID renewalOrderId,
        OrderType orderType,
        PaymentStatus paymentStatus,
        OrderStatus renewalStatus,
        RenewalOutboxStatus outboxStatus,
        String serviceDisplayName,
        String serviceUsername,
        Instant queuedAt,
        Instant lastUpdatedAt
) {
}
