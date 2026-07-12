package com.parazit.panel.application.payment.result;

import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentResult(
        UUID id,
        UUID orderId,
        UUID userId,
        PaymentMethod method,
        PaymentStatus status,
        long baseAmount,
        long payableAmount,
        String currency,
        Instant expiresAt,
        Instant paidAt,
        Instant approvedAt,
        Instant rejectedAt,
        String gatewayTransactionId,
        String gatewayAuthority,
        String rejectionReason,
        Instant createdAt,
        Instant updatedAt
) {
}
