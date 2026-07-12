package com.parazit.panel.api.payment.manual.receipt;

import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import java.time.Instant;
import java.util.UUID;

public record ManualPaymentReviewQueueItemResponse(
        UUID receiptId,
        UUID paymentId,
        UUID instructionId,
        UUID userId,
        Long telegramUserId,
        long expectedAmount,
        long claimedAmount,
        String currency,
        String originalFilename,
        String detectedContentType,
        long fileSizeBytes,
        String claimedTrackingNumber,
        String claimedSenderCardLastFour,
        Instant claimedPaidAt,
        Instant submittedAt,
        Instant reviewQueuedAt,
        boolean duplicateHashDetected,
        PaymentStatus paymentStatus,
        ManualPaymentReceiptStatus receiptStatus
) {
}
