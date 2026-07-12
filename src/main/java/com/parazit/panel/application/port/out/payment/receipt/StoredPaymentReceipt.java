package com.parazit.panel.application.port.out.payment.receipt;

public record StoredPaymentReceipt(
        String storageProvider,
        String storageKey,
        String sanitizedFilename,
        String detectedContentType,
        long fileSizeBytes,
        String fileSha256
) {
}
