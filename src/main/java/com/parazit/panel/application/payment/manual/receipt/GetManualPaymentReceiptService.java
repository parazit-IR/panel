package com.parazit.panel.application.payment.manual.receipt;

import com.parazit.panel.application.payment.PaymentNotFoundException;
import com.parazit.panel.application.payment.manual.receipt.query.GetManualPaymentReceiptByIdQuery;
import com.parazit.panel.application.payment.manual.receipt.query.GetManualPaymentReceiptByPaymentQuery;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReceiptResult;
import com.parazit.panel.application.port.in.payment.manual.receipt.GetManualPaymentReceiptUseCase;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetManualPaymentReceiptService implements GetManualPaymentReceiptUseCase {

    private final ManualPaymentReceiptRepository receiptRepository;
    private final PaymentRepository paymentRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final UserRepository userRepository;
    private final ManualPaymentReceiptResultMapper mapper;

    public GetManualPaymentReceiptService(
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
    public ManualPaymentReceiptResult getById(GetManualPaymentReceiptByIdQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(query.receiptId(), "receiptId must not be null");
        if (query.telegramUserId() == null || query.telegramUserId() <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        User user = userRepository.findByTelegramUserId(query.telegramUserId())
                .orElseThrow(() -> new ManualPaymentReceiptNotFoundException(query.receiptId()));
        ManualPaymentReceipt receipt = receiptRepository.findById(query.receiptId())
                .orElseThrow(() -> new ManualPaymentReceiptNotFoundException(query.receiptId()));
        if (!receipt.getUserId().equals(user.getId())) {
            throw new ManualPaymentReceiptNotFoundException(query.receiptId());
        }
        Payment payment = paymentRepository.findById(receipt.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(receipt.getPaymentId()));
        ManualCardPaymentInstruction instruction = instructionRepository.findById(receipt.getInstructionId())
                .orElseThrow(() -> new ManualPaymentReceiptNotFoundException(query.receiptId()));
        return mapper.toReceiptResult(payment, instruction, receipt);
    }

    @Override
    @Transactional(readOnly = true)
    public ManualPaymentReceiptResult getCurrentByPayment(GetManualPaymentReceiptByPaymentQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(query.paymentId(), "paymentId must not be null");
        if (query.telegramUserId() == null || query.telegramUserId() <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        User user = userRepository.findByTelegramUserId(query.telegramUserId())
                .orElseThrow(() -> new PaymentNotFoundException(query.paymentId()));
        Payment payment = paymentRepository.findById(query.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(query.paymentId()));
        if (!payment.getUserId().equals(user.getId())) {
            throw new PaymentNotFoundException(query.paymentId());
        }
        ManualPaymentReceipt receipt = receiptRepository.findAllByPaymentIdOrderBySubmittedAtDesc(payment.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ManualPaymentReceiptNotFoundException(payment.getId()));
        ManualCardPaymentInstruction instruction = instructionRepository.findById(receipt.getInstructionId())
                .orElseThrow(() -> new ManualPaymentReceiptNotFoundException(receipt.getId()));
        return mapper.toReceiptResult(payment, instruction, receipt);
    }
}
