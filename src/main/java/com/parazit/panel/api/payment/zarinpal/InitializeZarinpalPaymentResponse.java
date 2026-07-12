package com.parazit.panel.api.payment.zarinpal;

import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalAttemptStatus;
import java.time.Instant;
import java.util.UUID;

public record InitializeZarinpalPaymentResponse(
        UUID paymentId,
        UUID attemptId,
        UUID requestId,
        PaymentStatus paymentStatus,
        ZarinpalAttemptStatus attemptStatus,
        String paymentUrl,
        Instant expiresAt,
        boolean newlyInitialized
) {
}
