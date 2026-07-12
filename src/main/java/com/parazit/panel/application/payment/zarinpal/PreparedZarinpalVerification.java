package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;

record PreparedZarinpalVerification(
        Payment payment,
        ZarinpalPaymentAttempt attempt,
        boolean alreadyApproved,
        boolean cancelled
) {
}
