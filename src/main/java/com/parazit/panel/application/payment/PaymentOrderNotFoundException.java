package com.parazit.panel.application.payment;

import java.util.UUID;

public class PaymentOrderNotFoundException extends RuntimeException {

    public PaymentOrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId);
    }
}
