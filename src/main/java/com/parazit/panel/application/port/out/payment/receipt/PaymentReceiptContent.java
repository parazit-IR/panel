package com.parazit.panel.application.port.out.payment.receipt;

public record PaymentReceiptContent(
        String filename,
        String contentType,
        long sizeBytes,
        ReceiptUploadSource contentSource
) {
}
