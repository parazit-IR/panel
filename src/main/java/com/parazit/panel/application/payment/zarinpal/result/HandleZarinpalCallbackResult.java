package com.parazit.panel.application.payment.zarinpal.result;

import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalAttemptStatus;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalCallbackOutcome;
import java.util.UUID;

public record HandleZarinpalCallbackResult(
        UUID paymentId,
        UUID attemptId,
        PaymentStatus paymentStatus,
        ZarinpalAttemptStatus attemptStatus,
        ZarinpalCallbackOutcome outcome,
        String referenceId,
        boolean newlyVerified
) {
}
