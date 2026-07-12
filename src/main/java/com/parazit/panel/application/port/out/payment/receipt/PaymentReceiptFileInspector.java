package com.parazit.panel.application.port.out.payment.receipt;

public interface PaymentReceiptFileInspector {

    InspectedPaymentReceiptFile inspect(
            ReceiptUploadSource uploadSource,
            String originalFilename,
            String declaredContentType,
            long declaredSizeBytes
    );
}
