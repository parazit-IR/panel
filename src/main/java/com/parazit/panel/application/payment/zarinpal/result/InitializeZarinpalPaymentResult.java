package com.parazit.panel.application.payment.zarinpal.result;

import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalAttemptStatus;
import java.time.Instant;
import java.util.UUID;

public record InitializeZarinpalPaymentResult(
        UUID paymentId,
        UUID attemptId,
        UUID requestId,
        PaymentStatus paymentStatus,
        ZarinpalAttemptStatus attemptStatus,
        String authority,
        String paymentUrl,
        Instant expiresAt,
        boolean newlyInitialized
) {
}
