package com.parazit.panel.application.payment.manual.receipt.result;

import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import java.time.Instant;
import java.util.UUID;

public record ManualPaymentReceiptResult(
        UUID receiptId,
        UUID receiptRequestId,
        UUID paymentId,
        UUID instructionId,
        ManualPaymentReceiptStatus receiptStatus,
        PaymentStatus paymentStatus,
        ManualPaymentInstructionStatus instructionStatus,
        String originalFilename,
        String sanitizedFilename,
        String detectedContentType,
        Long fileSizeBytes,
        String fileSha256Prefix,
        long claimedAmount,
        String claimedTrackingNumber,
        String claimedSenderCardLastFour,
        Instant claimedPaidAt,
        String userNote,
        Instant submittedAt,
        Instant reviewQueuedAt,
        boolean duplicateHashDetected
) {
}
