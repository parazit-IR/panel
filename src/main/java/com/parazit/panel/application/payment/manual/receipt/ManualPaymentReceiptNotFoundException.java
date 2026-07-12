package com.parazit.panel.application.payment.manual.receipt;

import java.util.UUID;

public class ManualPaymentReceiptNotFoundException extends RuntimeException {
    public ManualPaymentReceiptNotFoundException(UUID receiptId) {
        super("Manual payment receipt not found " + receiptId);
    }
}
