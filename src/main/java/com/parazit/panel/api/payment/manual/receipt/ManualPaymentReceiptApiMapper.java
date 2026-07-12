package com.parazit.panel.api.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.query.GetManualPaymentReceiptByIdQuery;
import com.parazit.panel.application.payment.manual.receipt.query.GetManualPaymentReceiptByPaymentQuery;
import com.parazit.panel.application.payment.manual.receipt.query.GetManualPaymentReceiptContentQuery;
import com.parazit.panel.application.payment.manual.receipt.query.ListManualPaymentReviewQueueQuery;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReceiptResult;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReviewQueueItemResult;
import com.parazit.panel.application.payment.manual.receipt.result.SubmitManualPaymentReceiptResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ManualPaymentReceiptApiMapper {

    public GetManualPaymentReceiptByIdQuery toGetQuery(UUID receiptId, Long telegramUserId) {
        return new GetManualPaymentReceiptByIdQuery(receiptId, telegramUserId);
    }

    public GetManualPaymentReceiptByPaymentQuery toPaymentQuery(UUID paymentId, Long telegramUserId) {
        return new GetManualPaymentReceiptByPaymentQuery(paymentId, telegramUserId);
    }

    public GetManualPaymentReceiptContentQuery toContentQuery(UUID receiptId) {
        return new GetManualPaymentReceiptContentQuery(receiptId);
    }

    public ListManualPaymentReviewQueueQuery toQueueQuery(Integer limit, Integer offset, Boolean duplicateOnly) {
        return new ListManualPaymentReviewQueueQuery(
                limit == null ? 50 : limit,
                offset == null ? 0 : offset,
                Boolean.TRUE.equals(duplicateOnly)
        );
    }

    public ManualPaymentReceiptResponse toResponse(SubmitManualPaymentReceiptResult result) {
        return toResponse(result.receipt(), result.newlySubmitted());
    }

    public ManualPaymentReceiptResponse toResponse(ManualPaymentReceiptResult result) {
        return toResponse(result, false);
    }

    private ManualPaymentReceiptResponse toResponse(ManualPaymentReceiptResult result, boolean newlySubmitted) {
        return new ManualPaymentReceiptResponse(
                result.receiptId(),
                result.receiptRequestId(),
                result.paymentId(),
                result.instructionId(),
                result.receiptStatus(),
                result.paymentStatus(),
                result.instructionStatus(),
                result.originalFilename(),
                result.sanitizedFilename(),
                result.detectedContentType(),
                result.fileSizeBytes(),
                result.fileSha256Prefix(),
                result.claimedAmount(),
                result.claimedTrackingNumber(),
                result.claimedSenderCardLastFour(),
                result.claimedPaidAt(),
                result.userNote(),
                result.submittedAt(),
                result.reviewQueuedAt(),
                result.duplicateHashDetected(),
                newlySubmitted
        );
    }

    public ManualPaymentReviewQueueItemResponse toResponse(ManualPaymentReviewQueueItemResult result) {
        return new ManualPaymentReviewQueueItemResponse(
                result.receiptId(),
                result.paymentId(),
                result.instructionId(),
                result.userId(),
                result.telegramUserId(),
                result.expectedAmount(),
                result.claimedAmount(),
                result.currency(),
                result.originalFilename(),
                result.detectedContentType(),
                result.fileSizeBytes(),
                result.claimedTrackingNumber(),
                result.claimedSenderCardLastFour(),
                result.claimedPaidAt(),
                result.submittedAt(),
                result.reviewQueuedAt(),
                result.duplicateHashDetected(),
                result.paymentStatus(),
                result.receiptStatus()
        );
    }
}
