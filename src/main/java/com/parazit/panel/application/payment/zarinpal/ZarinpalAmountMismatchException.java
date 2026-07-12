package com.parazit.panel.application.payment.zarinpal;

public class ZarinpalAmountMismatchException extends RuntimeException {
    public ZarinpalAmountMismatchException() {
        super("Zarinpal amount does not match persisted payment");
    }
}
