package com.parazit.panel.application.port.out.payment.receipt;

public record InspectedPaymentReceiptFile(
        String detectedContentType,
        String normalizedExtension,
        long sizeBytes,
        String sha256,
        Integer imageWidth,
        Integer imageHeight
) {
}
