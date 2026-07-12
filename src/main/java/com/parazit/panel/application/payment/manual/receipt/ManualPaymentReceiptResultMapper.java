package com.parazit.panel.application.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReceiptContentResult;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReceiptResult;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReviewQueueItemResult;
import com.parazit.panel.application.payment.manual.receipt.result.SubmitManualPaymentReceiptResult;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptContent;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.user.User;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ManualPaymentReceiptResultMapper {

    public SubmitManualPaymentReceiptResult toSubmitResult(
            Payment payment,
            ManualCardPaymentInstruction instruction,
            ManualPaymentReceipt receipt,
            boolean newlySubmitted
    ) {
        return new SubmitManualPaymentReceiptResult(toReceiptResult(payment, instruction, receipt), newlySubmitted);
    }

    public ManualPaymentReceiptResult toReceiptResult(
            Payment payment,
            ManualCardPaymentInstruction instruction,
            ManualPaymentReceipt receipt
    ) {
        Objects.requireNonNull(payment, "payment must not be null");
        Objects.requireNonNull(instruction, "instruction must not be null");
        Objects.requireNonNull(receipt, "receipt must not be null");
        String hash = receipt.getFileSha256();
        String hashPrefix = hash == null ? null : hash.substring(0, Math.min(hash.length(), 12));
        return new ManualPaymentReceiptResult(
                receipt.getId(),
                receipt.getReceiptRequestId(),
                payment.getId(),
                instruction.getId(),
                receipt.getStatus(),
                payment.getStatus(),
                instruction.getStatus(),
                receipt.getOriginalFilename(),
                receipt.getSanitizedFilename(),
                receipt.getDetectedContentType(),
                receipt.getFileSizeBytes(),
                hashPrefix,
                receipt.getClaimedAmount(),
                receipt.getClaimedTrackingNumber(),
                receipt.getClaimedSenderCardLastFour(),
                receipt.getClaimedPaidAt(),
                receipt.getUserNote(),
                receipt.getSubmittedAt(),
                receipt.getReviewQueuedAt(),
                receipt.isDuplicateHashDetected()
        );
    }

    public ManualPaymentReceiptContentResult toContentResult(PaymentReceiptContent content) {
        return new ManualPaymentReceiptContentResult(
                content.filename(),
                content.contentType(),
                content.sizeBytes(),
                content.contentSource()
        );
    }

    public ManualPaymentReviewQueueItemResult toQueueItem(
            ManualPaymentReceipt receipt,
            Payment payment,
            ManualCardPaymentInstruction instruction,
            User user
    ) {
        return new ManualPaymentReviewQueueItemResult(
                receipt.getId(),
                payment.getId(),
                instruction.getId(),
                user.getId(),
                user.getTelegramUserId(),
                instruction.getPayableAmount(),
                receipt.getClaimedAmount(),
                payment.getCurrency(),
                receipt.getOriginalFilename(),
                receipt.getDetectedContentType(),
                receipt.getFileSizeBytes() == null ? 0 : receipt.getFileSizeBytes(),
                receipt.getClaimedTrackingNumber(),
                receipt.getClaimedSenderCardLastFour(),
                receipt.getClaimedPaidAt(),
                receipt.getSubmittedAt(),
                receipt.getReviewQueuedAt(),
                receipt.isDuplicateHashDetected(),
                payment.getStatus(),
                receipt.getStatus()
        );
    }
}
