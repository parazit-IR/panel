package com.parazit.panel.application.payment.manual.receipt;

import com.parazit.panel.application.payment.PaymentNotFoundException;
import com.parazit.panel.application.payment.PaymentOrderNotFoundException;
import com.parazit.panel.application.payment.manual.ManualPaymentInstructionNotFoundException;
import com.parazit.panel.application.payment.manual.receipt.command.SubmitManualPaymentReceiptCommand;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceipt;
import com.parazit.panel.domain.payment.manual.receipt.ManualPaymentReceiptStatus;
import com.parazit.panel.domain.payment.manual.receipt.repository.ManualPaymentReceiptRepository;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PrepareManualPaymentReceiptTransaction {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final ManualPaymentReceiptRepository receiptRepository;
    private final SystemClockPort clock;

    public PrepareManualPaymentReceiptTransaction(
            UserRepository userRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            ManualPaymentReceiptRepository receiptRepository,
            SystemClockPort clock
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.instructionRepository = Objects.requireNonNull(instructionRepository, "instructionRepository must not be null");
        this.receiptRepository = Objects.requireNonNull(receiptRepository, "receiptRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public PreparedManualPaymentReceipt prepare(SubmitManualPaymentReceiptCommand command) {
        validateCommand(command);
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        return receiptRepository.findByReceiptRequestId(command.receiptRequestId())
                .map(existing -> replay(command, existing, user))
                .orElseGet(() -> create(command, user));
    }

    private PreparedManualPaymentReceipt replay(
            SubmitManualPaymentReceiptCommand command,
            ManualPaymentReceipt receipt,
            User user
    ) {
        Payment payment = loadOwnedPayment(command.paymentId(), user);
        if (!receipt.getPaymentId().equals(payment.getId())
                || receipt.getClaimedAmount() != command.claimedAmount()) {
            throw new ManualPaymentReceiptRequestIdConflictException();
        }
        ManualCardPaymentInstruction instruction = instructionRepository.findById(receipt.getInstructionId())
                .orElseThrow(() -> new ManualPaymentInstructionNotFoundException(payment.getId()));
        if (receipt.getStatus() == ManualPaymentReceiptStatus.INVALID_FILE
                || receipt.getStatus() == ManualPaymentReceiptStatus.UPLOADING) {
            throw new ManualPaymentReceiptSubmissionNotAllowedException("Receipt submission is not complete");
        }
        return new PreparedManualPaymentReceipt(payment, instruction, receipt, user, true);
    }

    private PreparedManualPaymentReceipt create(SubmitManualPaymentReceiptCommand command, User user) {
        Payment payment = loadPayment(command.paymentId(), user);
        ManualCardPaymentInstruction instruction = instructionRepository.findActiveByPaymentId(payment.getId())
                .orElseThrow(() -> new ManualPaymentInstructionNotFoundException(payment.getId()));
        Instant now = clock.now();
        if (instruction.getStatus() != ManualPaymentInstructionStatus.ACTIVE || instruction.isExpiredAt(now)) {
            throw new ManualPaymentReceiptSubmissionNotAllowedException("Manual payment instruction is not active");
        }
        if (command.claimedAmount() != instruction.getPayableAmount()) {
            throw new ManualPaymentReceiptAmountMismatchException();
        }
        if (receiptRepository.existsActiveByInstructionId(instruction.getId())) {
            throw new ManualPaymentReceiptAlreadySubmittedException();
        }
        ManualPaymentReceipt receipt = ManualPaymentReceipt.createUploading(
                command.receiptRequestId(),
                payment.getId(),
                instruction.getId(),
                user.getId(),
                command.originalFilename(),
                command.claimedAmount(),
                command.claimedTrackingNumber(),
                command.claimedSenderCardLastFour(),
                command.claimedPaidAt(),
                command.userNote(),
                now
        );
        try {
            return new PreparedManualPaymentReceipt(payment, instruction, receiptRepository.save(receipt), user, false);
        } catch (DataIntegrityViolationException exception) {
            throw new ManualPaymentReceiptAlreadySubmittedException();
        }
    }

    private Payment loadPayment(java.util.UUID paymentId, User user) {
        if (user.getStatus() != UserStatus.ACTIVE || Boolean.TRUE.equals(user.getBlocked())) {
            throw new ManualPaymentReceiptSubmissionNotAllowedException("User is not eligible to submit a receipt");
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (!payment.getUserId().equals(user.getId())) {
            throw new PaymentNotFoundException(paymentId);
        }
        if (payment.getMethod() != PaymentMethod.CARD_TO_CARD) {
            throw new ManualPaymentReceiptSubmissionNotAllowedException("Receipts are only accepted for manual card payments");
        }
        if (payment.getStatus() != PaymentStatus.WAITING_FOR_PAYMENT) {
            throw new ManualPaymentReceiptSubmissionNotAllowedException("Payment is not waiting for manual payment");
        }
        if (!payment.getExpiresAt().isAfter(clock.now())) {
            throw new ManualPaymentReceiptSubmissionNotAllowedException("Payment is expired");
        }
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new PaymentOrderNotFoundException(payment.getOrderId()));
        if (!order.getUserId().equals(user.getId())) {
            throw new ManualPaymentReceiptSubmissionNotAllowedException("Order owner does not match payment owner");
        }
        return payment;
    }

    private Payment loadOwnedPayment(java.util.UUID paymentId, User user) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (!payment.getUserId().equals(user.getId())) {
            throw new PaymentNotFoundException(paymentId);
        }
        return payment;
    }

    private static void validateCommand(SubmitManualPaymentReceiptCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.receiptRequestId(), "receiptRequestId must not be null");
        Objects.requireNonNull(command.paymentId(), "paymentId must not be null");
        Objects.requireNonNull(command.uploadSource(), "uploadSource must not be null");
        if (command.telegramUserId() == null || command.telegramUserId() <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        if (command.declaredSizeBytes() <= 0 || command.claimedAmount() <= 0) {
            throw new IllegalArgumentException("file size and claimed amount must be positive");
        }
    }
}
