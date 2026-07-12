package com.parazit.panel.application.payment.manual.receipt.result;

import com.parazit.panel.application.port.out.payment.receipt.ReceiptUploadSource;

public record ManualPaymentReceiptContentResult(
        String filename,
        String contentType,
        long sizeBytes,
        ReceiptUploadSource contentSource
) {
}
