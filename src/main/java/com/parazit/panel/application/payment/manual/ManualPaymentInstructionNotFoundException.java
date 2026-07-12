package com.parazit.panel.application.payment.manual;

import java.util.UUID;

public class ManualPaymentInstructionNotFoundException extends RuntimeException {

    public ManualPaymentInstructionNotFoundException(UUID paymentId) {
        super("Manual payment instruction not found for payment " + paymentId);
    }
}
