package com.parazit.panel.application.payment.manual.receipt;

import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.user.User;

record PreparedManualPaymentReceipt(
        Payment payment,
        ManualCardPaymentInstruction instruction,
        ManualPaymentReceipt receipt,
        User user,
        boolean replay
) {
}
