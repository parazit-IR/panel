package com.parazit.panel.application.payment.manual;

import com.parazit.panel.application.payment.PaymentNotFoundException;
import com.parazit.panel.application.payment.manual.command.CancelManualCardPaymentInstructionCommand;
import com.parazit.panel.application.payment.manual.query.GetManualCardPaymentInstructionQuery;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.payment.manual.ManualPaymentDestinationProvider;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;
import com.parazit.panel.domain.payment.manual.ManualPaymentInstructionStatus;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ManualPaymentLookupTransaction {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final ManualPaymentDestinationProvider destinationProvider;
    private final SystemClockPort clock;

    public ManualPaymentLookupTransaction(
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            ManualPaymentDestinationProvider destinationProvider,
            SystemClockPort clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.instructionRepository = Objects.requireNonNull(instructionRepository, "instructionRepository must not be null");
        this.destinationProvider = Objects.requireNonNull(destinationProvider, "destinationProvider must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ManualCardPaymentReservationResult getCurrent(GetManualCardPaymentInstructionQuery query) {
        validateQuery(query);
        Payment payment = loadOwnedPayment(query.paymentId(), query.telegramUserId());
        ManualCardPaymentInstruction instruction = instructionRepository.findActiveByPaymentId(payment.getId())
                .orElseThrow(() -> new ManualPaymentInstructionNotFoundException(payment.getId()));
        Instant now = clock.now();
        if (instruction.isExpiredAt(now)) {
            instruction.expire(now);
            instructionRepository.save(instruction);
            throw new ManualPaymentInstructionNotFoundException(payment.getId());
        }
        return new ManualCardPaymentReservationResult(payment, instruction, destination(), false);
    }

    @Transactional
    public ManualCardPaymentReservationResult cancel(CancelManualCardPaymentInstructionCommand command) {
        validateCommand(command);
        Payment payment = loadOwnedPayment(command.paymentId(), command.telegramUserId());
        ManualCardPaymentInstruction instruction = instructionRepository.findActiveByPaymentId(payment.getId())
                .orElseGet(() -> instructionRepository.findAllByPaymentIdOrderByCreatedAtDesc(payment.getId())
                        .stream()
                        .filter(item -> item.getStatus() == ManualPaymentInstructionStatus.CANCELLED)
                        .findFirst()
                        .orElseThrow(() -> new ManualPaymentInstructionNotFoundException(payment.getId())));
        Instant now = clock.now();
        if (instruction.isActiveReservation()) {
            if (instruction.isExpiredAt(now)) {
                instruction.expire(now);
            } else {
                instruction.cancel(now);
            }
            instructionRepository.save(instruction);
        }
        return new ManualCardPaymentReservationResult(payment, instruction, destination(), false);
    }

    private Payment loadOwnedPayment(java.util.UUID paymentId, Long telegramUserId) {
        User user = userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (!payment.getUserId().equals(user.getId())) {
            throw new PaymentNotFoundException(paymentId);
        }
        return payment;
    }

    private ManualPaymentDestination destination() {
        return destinationProvider.firstActiveDestination()
                .orElseThrow(ManualPaymentDestinationUnavailableException::new);
    }

    private static void validateQuery(GetManualCardPaymentInstructionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(query.paymentId(), "paymentId must not be null");
        if (query.telegramUserId() == null || query.telegramUserId() <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
    }

    private static void validateCommand(CancelManualCardPaymentInstructionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.paymentId(), "paymentId must not be null");
        if (command.telegramUserId() == null || command.telegramUserId() <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
    }
}
