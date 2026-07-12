package com.parazit.panel.application.payment.manual.receipt;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.payment.receipt.StoredPaymentReceipt;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CompleteManualPaymentReceiptTransaction {

    private final PaymentRepository paymentRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final ManualPaymentReceiptRepository receiptRepository;
    private final SystemClockPort clock;

    public CompleteManualPaymentReceiptTransaction(
            PaymentRepository paymentRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            ManualPaymentReceiptRepository receiptRepository,
            SystemClockPort clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.instructionRepository = Objects.requireNonNull(instructionRepository, "instructionRepository must not be null");
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public PreparedManualPaymentReceipt complete(
            UUID receiptId,
            StoredPaymentReceipt stored,
            boolean duplicateHashDetected
    ) {
        ManualPaymentReceipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ManualPaymentReceiptNotFoundException(receiptId));
        Payment payment = paymentRepository.findById(receipt.getPaymentId())
                .orElseThrow(() -> new ManualPaymentReceiptNotFoundException(receiptId));
        ManualCardPaymentInstruction instruction = instructionRepository.findById(receipt.getInstructionId())
                .orElseThrow(() -> new ManualPaymentReceiptNotFoundException(receiptId));

        receipt.markStored(
                stored.storageProvider(),
                stored.storageKey(),
                stored.sanitizedFilename(),
                stored.detectedContentType(),
                stored.fileSizeBytes(),
                stored.fileSha256(),
                duplicateHashDetected
        );
        receipt.queueForReview(clock.now());
        instruction.markReceiptPending(clock.now());
        payment.markReceiptSubmitted(clock.now());
        payment.markWaitingForReview();

        receiptRepository.save(receipt);
        instructionRepository.save(instruction);
        paymentRepository.save(payment);
        return new PreparedManualPaymentReceipt(payment, instruction, receipt, null, false);
    }

    @Transactional
    public void markInvalid(UUID receiptId, String safeReason) {
        receiptRepository.findById(receiptId).ifPresent(receipt -> {
            receipt.markInvalidFile(safeReason);
            receiptRepository.save(receipt);
        });
    }
}
