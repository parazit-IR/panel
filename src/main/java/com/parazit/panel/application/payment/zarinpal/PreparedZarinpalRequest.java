package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;

record PreparedZarinpalRequest(
        Payment payment,
        ZarinpalPaymentAttempt attempt,
        boolean replay
) {
}
