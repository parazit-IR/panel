package com.parazit.panel.application.payment;

import com.parazit.panel.domain.payment.PaymentMethod;

public class PaymentProcessorNotFoundException extends RuntimeException {

    public PaymentProcessorNotFoundException(PaymentMethod method) {
        super("No payment processor is configured for method " + method);
    }
}
