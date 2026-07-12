package com.parazit.panel.application.payment.zarinpal;

public class ZarinpalPaymentNotAllowedException extends RuntimeException {
    public ZarinpalPaymentNotAllowedException(String message) {
        super(message);
    }
}
