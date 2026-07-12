package com.parazit.panel.application.payment;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ApprovePaymentCommand(
        UUID paymentId,
        PaymentApprovalSource source,
        String providerReference,
        String providerAuthority,
        Instant approvedAt
) {

    public ApprovePaymentCommand {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(approvedAt, "approvedAt must not be null");
        providerReference = providerReference == null ? null : providerReference.trim();
        if (providerReference != null && providerReference.isBlank()) {
            providerReference = null;
        }
        providerAuthority = providerAuthority == null ? null : providerAuthority.trim();
        if (providerAuthority != null && providerAuthority.isBlank()) {
            providerAuthority = null;
        }
    }
}
