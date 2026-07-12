package com.parazit.panel.application.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.command.SubmitManualPaymentReceiptCommand;
import com.parazit.panel.application.payment.manual.receipt.result.SubmitManualPaymentReceiptResult;
import com.parazit.panel.application.port.in.payment.manual.receipt.SubmitManualPaymentReceiptUseCase;
import com.parazit.panel.application.port.out.payment.receipt.InspectedPaymentReceiptFile;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptFileInspector;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptStorage;
import com.parazit.panel.application.port.out.payment.receipt.StorePaymentReceiptCommand;
import com.parazit.panel.application.port.out.payment.receipt.StoredPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SubmitManualPaymentReceiptService implements SubmitManualPaymentReceiptUseCase {

    private static final Logger log = LoggerFactory.getLogger(SubmitManualPaymentReceiptService.class);

    private final PrepareManualPaymentReceiptTransaction prepareTransaction;
    private final CompleteManualPaymentReceiptTransaction completeTransaction;
    private final ManualPaymentReceiptRepository receiptRepository;
    private final PaymentReceiptFileInspector fileInspector;
    private final PaymentReceiptFilenameSanitizer filenameSanitizer;
    private final PaymentReceiptStorage storage;
    private final ManualPaymentReceiptResultMapper mapper;

    public SubmitManualPaymentReceiptService(
            PrepareManualPaymentReceiptTransaction prepareTransaction,
            CompleteManualPaymentReceiptTransaction completeTransaction,
            ManualPaymentReceiptRepository receiptRepository,
            PaymentReceiptFileInspector fileInspector,
            PaymentReceiptFilenameSanitizer filenameSanitizer,
            PaymentReceiptStorage storage,
            ManualPaymentReceiptResultMapper mapper
    ) {
        this.prepareTransaction = Objects.requireNonNull(prepareTransaction, "prepareTransaction must not be null");
        this.completeTransaction = Objects.requireNonNull(completeTransaction, "completeTransaction must not be null");
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.fileInspector = Objects.requireNonNull(fileInspector, "fileInspector must not be null");
        this.filenameSanitizer = Objects.requireNonNull(filenameSanitizer, "filenameSanitizer must not be null");
        this.storage = Objects.requireNonNull(storage, "storage must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public SubmitManualPaymentReceiptResult submit(SubmitManualPaymentReceiptCommand command) {
        PreparedManualPaymentReceipt prepared = prepareTransaction.prepare(command);
        if (prepared.replay()) {
            log.info("Manual receipt returned idempotently receiptId={}", prepared.receipt().getId());
            return mapper.toSubmitResult(prepared.payment(), prepared.instruction(), prepared.receipt(), false);
        }

        ManualPaymentReceipt receipt = prepared.receipt();
        StoredPaymentReceipt stored = null;
        try {
            InspectedPaymentReceiptFile inspected = fileInspector.inspect(
                    command.uploadSource(),
                    command.originalFilename(),
                    command.declaredContentType(),
                    command.declaredSizeBytes()
            );
            receiptRepository.findActiveByUserIdAndFileSha256(prepared.user().getId(), inspected.sha256())
                    .filter(existing -> !existing.getId().equals(receipt.getId()))
                    .ifPresent(existing -> {
                        throw new ManualPaymentReceiptDuplicateException();
                    });
            boolean duplicateHashDetected = receiptRepository.findActiveByFileSha256(inspected.sha256())
                    .filter(existing -> !existing.getUserId().equals(prepared.user().getId()))
                    .isPresent();
            String sanitizedFilename = filenameSanitizer.sanitize(command.originalFilename(), inspected.normalizedExtension());
            stored = storage.store(new StorePaymentReceiptCommand(
                    receipt.getId(),
                    command.originalFilename(),
                    sanitizedFilename,
                    inspected.detectedContentType(),
                    inspected.sizeBytes(),
                    inspected.sha256(),
                    command.uploadSource()
            ));
            PreparedManualPaymentReceipt completed = completeTransaction.complete(
                    receipt.getId(),
                    stored,
                    duplicateHashDetected
            );
            log.info("Manual receipt queued for review receiptId={} paymentId={}", receipt.getId(), prepared.payment().getId());
            return mapper.toSubmitResult(completed.payment(), completed.instruction(), completed.receipt(), true);
        } catch (ManualPaymentReceiptInvalidFileException
                 | ManualPaymentReceiptUnsupportedTypeException
                 | ManualPaymentReceiptFileTooLargeException
                 | ManualPaymentReceiptDuplicateException exception) {
            completeTransaction.markInvalid(receipt.getId(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            if (stored != null) {
                try {
                    storage.delete(stored.storageKey());
                } catch (RuntimeException compensationFailure) {
                    log.warn("Receipt storage compensation failed receiptId={}", receipt.getId());
                }
            }
            throw exception;
        }
    }
}
