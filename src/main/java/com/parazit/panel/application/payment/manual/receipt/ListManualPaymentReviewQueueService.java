package com.parazit.panel.application.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.query.ListManualPaymentReviewQueueQuery;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReviewQueueItemResult;
import com.parazit.panel.application.port.in.payment.manual.receipt.ListManualPaymentReviewQueueUseCase;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListManualPaymentReviewQueueService implements ListManualPaymentReviewQueueUseCase {

    private final ManualPaymentReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final UserRepository userRepository;
    private final ManualPaymentReceiptResultMapper mapper;

    public ListManualPaymentReviewQueueService(
            ManualPaymentReceiptRepository receiptRepository,
            PaymentRepository paymentRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            UserRepository userRepository,
            ManualPaymentReceiptResultMapper mapper
    ) {
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.instructionRepository = Objects.requireNonNull(instructionRepository, "instructionRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ManualPaymentReviewQueueItemResult> list(ListManualPaymentReviewQueueQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        int limit = Math.max(1, Math.min(query.limit() <= 0 ? 50 : query.limit(), 100));
        int offset = Math.max(0, query.offset());
        return receiptRepository.findAllQueuedForReviewOrderByReviewQueuedAtAsc(limit, offset)
                .stream()
                .filter(receipt -> !query.duplicateOnly() || receipt.isDuplicateHashDetected())
                .map(this::toQueueItem)
                .toList();
    }

    private ManualPaymentReviewQueueItemResult toQueueItem(ManualPaymentReceipt receipt) {
        Payment payment = paymentRepository.findById(receipt.getPaymentId()).orElseThrow();
        ManualCardPaymentInstruction instruction = instructionRepository.findById(receipt.getInstructionId()).orElseThrow();
        User user = userRepository.findById(receipt.getUserId()).orElseThrow();
        return mapper.toQueueItem(receipt, payment, instruction, user);
    }
}
