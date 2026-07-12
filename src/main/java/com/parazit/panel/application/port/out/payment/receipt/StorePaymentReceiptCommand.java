package com.parazit.panel.application.port.out.payment.receipt;

import java.util.Objects;
import java.util.UUID;

public record StorePaymentReceiptCommand(
        UUID receiptId,
        String originalFilename,
        String sanitizedFilename,
        String detectedContentType,
        long fileSizeBytes,
        String fileSha256,
        ReceiptUploadSource uploadSource
) {

    public StorePaymentReceiptCommand {
        Objects.requireNonNull(receiptId, "receiptId must not be null");
        Objects.requireNonNull(originalFilename, "originalFilename must not be null");
        Objects.requireNonNull(sanitizedFilename, "sanitizedFilename must not be null");
        Objects.requireNonNull(detectedContentType, "detectedContentType must not be null");
        Objects.requireNonNull(fileSha256, "fileSha256 must not be null");
        Objects.requireNonNull(uploadSource, "uploadSource must not be null");
        if (fileSizeBytes <= 0) {
            throw new IllegalArgumentException("fileSizeBytes must be positive");
        }
    }
}
