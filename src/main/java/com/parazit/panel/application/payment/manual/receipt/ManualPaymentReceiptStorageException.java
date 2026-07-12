package com.parazit.panel.application.payment.manual.receipt;

public class ManualPaymentReceiptStorageException extends RuntimeException {
    public ManualPaymentReceiptStorageException(String message) {
        super(message);
    }

    public ManualPaymentReceiptStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
