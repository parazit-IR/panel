package com.parazit.panel.application.renewal.result;

import com.parazit.panel.domain.renewal.RenewalApprovalOutcome;
import java.time.Instant;
import java.util.UUID;

public record RenewalPaymentApprovalResult(
        UUID orderId,
        UUID paymentId,
        UUID renewalOutboxId,
        RenewalApprovalOutcome outcome,
        boolean created,
        boolean replayed,
        Instant approvedAt,
        Instant renewalRequestedAt
) {
}
