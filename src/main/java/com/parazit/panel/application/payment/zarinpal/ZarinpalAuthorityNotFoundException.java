package com.parazit.panel.application.payment.zarinpal;

public class ZarinpalAuthorityNotFoundException extends RuntimeException {
    public ZarinpalAuthorityNotFoundException() {
        super("Zarinpal authority not found");
    }
}
