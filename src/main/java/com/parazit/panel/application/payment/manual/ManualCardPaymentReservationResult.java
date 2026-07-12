package com.parazit.panel.application.payment.manual;

import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;

record ManualCardPaymentReservationResult(
        Payment payment,
        ManualCardPaymentInstruction instruction,
        ManualPaymentDestination destination,
        boolean newlyInitialized
) {
}
