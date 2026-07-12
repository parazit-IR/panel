package com.parazit.panel.application.payment.zarinpal;

public class ZarinpalDisabledException extends RuntimeException {
    public ZarinpalDisabledException() {
        super("Zarinpal payment is disabled");
    }
}
