package com.parazit.panel.application.payment.manual.receipt.command;

import com.parazit.panel.application.port.out.payment.receipt.ReceiptUploadSource;
import java.time.Instant;
import java.util.UUID;

public record SubmitManualPaymentReceiptCommand(
        UUID receiptRequestId,
        Long telegramUserId,
        UUID paymentId,
        String originalFilename,
        String declaredContentType,
        long declaredSizeBytes,
        long claimedAmount,
        String claimedTrackingNumber,
        String claimedSenderCardLastFour,
        Instant claimedPaidAt,
        String userNote,
        ReceiptUploadSource uploadSource
) {
}
