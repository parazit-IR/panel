package com.parazit.panel.application.port.out.payment.receipt;

public interface PaymentReceiptStorage {

    StoredPaymentReceipt store(StorePaymentReceiptCommand command);

    PaymentReceiptContent load(String storageKey);

    void delete(String storageKey);

    boolean exists(String storageKey);
}
