package com.parazit.panel.application.payment.manual;

import com.parazit.panel.application.payment.PaymentNotFoundException;
import com.parazit.panel.application.payment.PaymentOrderNotFoundException;
import com.parazit.panel.application.payment.manual.command.InitializeManualCardPaymentCommand;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.payment.manual.ManualPaymentDestinationProvider;
import com.parazit.panel.config.properties.ManualPaymentProperties;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.manual.ManualCardPaymentInstruction;
import com.parazit.panel.domain.payment.manual.ManualPaymentDestination;
import com.parazit.panel.domain.payment.manual.repository.ManualCardPaymentInstructionRepository;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.wallet.topup.WalletTopUpRequest;
import com.parazit.panel.domain.wallet.topup.repository.WalletTopUpRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ManualCardPaymentReservationTransaction {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final WalletTopUpRequestRepository walletTopUpRequestRepository;
    private final ManualCardPaymentInstructionRepository instructionRepository;
    private final ManualPaymentDestinationProvider destinationProvider;
    private final ManualPaymentProperties properties;
    private final SystemClockPort clock;

    public ManualCardPaymentReservationTransaction(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            UserRepository userRepository,
            WalletTopUpRequestRepository walletTopUpRequestRepository,
            ManualCardPaymentInstructionRepository instructionRepository,
            ManualPaymentDestinationProvider destinationProvider,
            ManualPaymentProperties properties,
            SystemClockPort clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.walletTopUpRequestRepository = Objects.requireNonNull(walletTopUpRequestRepository, "walletTopUpRequestRepository must not be null");
        this.instructionRepository = Objects.requireNonNull(instructionRepository, "instructionRepository must not be null");
        this.destinationProvider = Objects.requireNonNull(destinationProvider, "destinationProvider must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ManualCardPaymentReservationResult reserve(
            InitializeManualCardPaymentCommand command,
            long uniqueSuffixAmount
    ) {
        validateCommand(command);
        if (!properties.enabled()) {
            throw new ManualCardPaymentDisabledException();
        }
        ManualPaymentDestination destination = destination();
        Instant now = clock.now();
        return instructionRepository.findByInstructionRequestId(command.instructionRequestId())
                .map(existing -> replay(command, existing, destination))
                .orElseGet(() -> createOrReturnActive(command, uniqueSuffixAmount, destination, now));
    }

    private ManualCardPaymentReservationResult replay(
            InitializeManualCardPaymentCommand command,
            ManualCardPaymentInstruction instruction,
            ManualPaymentDestination destination
    ) {
        Payment payment = loadAndValidatePayment(command.paymentId(), command.telegramUserId());
        if (!instruction.getPaymentId().equals(payment.getId())) {
            throw new ManualPaymentRequestIdConflictException();
        }
        return new ManualCardPaymentReservationResult(payment, instruction, destination, false);
    }

    private ManualCardPaymentReservationResult createOrReturnActive(
            InitializeManualCardPaymentCommand command,
            long uniqueSuffixAmount,
            ManualPaymentDestination destination,
            Instant now
    ) {
        Payment payment = loadAndValidatePayment(command.paymentId(), command.telegramUserId());
        instructionRepository.findActiveByPaymentId(payment.getId())
                .ifPresent(active -> {
                    if (active.isExpiredAt(now)) {
                        active.expire(now);
                        instructionRepository.save(active);
                    }
                });

        return instructionRepository.findActiveByPaymentId(payment.getId())
                .map(active -> new ManualCardPaymentReservationResult(payment, active, destination, false))
                .orElseGet(() -> createNewInstruction(command, payment, uniqueSuffixAmount, destination, now));
    }

    private ManualCardPaymentReservationResult createNewInstruction(
            InitializeManualCardPaymentCommand command,
            Payment payment,
            long uniqueSuffixAmount,
            ManualPaymentDestination destination,
            Instant now
    ) {
        enforceReissuePolicy(payment.getId(), now);
        ManualCardPaymentInstruction instruction = ManualCardPaymentInstruction.create(
                payment.getId(),
                command.instructionRequestId(),
                payment.getPayableAmount(),
                uniqueSuffixAmount,
                payment.getCurrency(),
                destination,
                now,
                properties.instructionTtl()
        );
        instruction.activate();
        payment.markWaitingForPayment();
        paymentRepository.save(payment);
        ManualCardPaymentInstruction saved = instructionRepository.save(instruction);
        return new ManualCardPaymentReservationResult(payment, saved, destination, true);
    }

    private void enforceReissuePolicy(java.util.UUID paymentId, Instant now) {
        List<ManualCardPaymentInstruction> history = instructionRepository.findAllByPaymentIdOrderByCreatedAtDesc(paymentId);
        if (history.isEmpty()) {
            return;
        }
        if (!properties.allowInstructionReissue()) {
            throw new ManualPaymentReissueNotAllowedException("Manual payment instruction reissue is disabled");
        }
        ManualCardPaymentInstruction latest = history.get(0);
        Instant reference = latest.getCancelledAt() != null
                ? latest.getCancelledAt()
                : latest.getExpiredAt() != null ? latest.getExpiredAt() : latest.getCreatedAt();
        if (reference != null && now.isBefore(reference.plus(properties.reissueCooldown()))) {
            throw new ManualPaymentReissueNotAllowedException("Manual payment instruction reissue is cooling down");
        }
    }

    private Payment loadAndValidatePayment(java.util.UUID paymentId, Long telegramUserId) {
        User user = userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (user.getStatus() != UserStatus.ACTIVE || Boolean.TRUE.equals(user.getBlocked())) {
            throw new ManualCardPaymentNotAllowedException("User is not eligible for manual payment");
        }
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (!payment.getUserId().equals(user.getId())) {
            throw new PaymentNotFoundException(paymentId);
        }
        if (payment.getMethod() != PaymentMethod.CARD_TO_CARD) {
            throw new ManualCardPaymentNotAllowedException("Payment method is not CARD_TO_CARD");
        }
        if (payment.getStatus() != PaymentStatus.CREATED && payment.getStatus() != PaymentStatus.WAITING_FOR_PAYMENT) {
            throw new ManualCardPaymentNotAllowedException("Payment is not payable with status " + payment.getStatus());
        }
        if (payment.getPayableAmount() <= 0) {
            throw new ManualCardPaymentNotAllowedException("Payment amount must be positive");
        }
        validateTarget(payment, user);
        if (!payment.getExpiresAt().isAfter(clock.now())) {
            throw new ManualCardPaymentNotAllowedException("Payment is expired");
        }
        return payment;
    }

    private void validateTarget(Payment payment, User user) {
        if (payment.targetsOrder()) {
            Order order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new PaymentOrderNotFoundException(payment.getOrderId()));
            if (!order.getUserId().equals(user.getId())) {
                throw new ManualPaymentInstructionConflictException("Order owner does not match payment owner");
            }
            if (paymentRepository.existsApprovedPaymentForOrder(order.getId())) {
                throw new ManualPaymentInstructionConflictException("Order already has an approved payment");
            }
            return;
        }
        if (payment.getWalletTopUpRequestId() == null) {
            throw new ManualPaymentInstructionConflictException("Payment target is invalid");
        }
        WalletTopUpRequest request = walletTopUpRequestRepository.findById(payment.getWalletTopUpRequestId())
                .orElseThrow(() -> new ManualPaymentInstructionConflictException("Wallet top-up request could not be found"));
        if (!request.getUserId().equals(user.getId())
                || !request.getId().equals(payment.getWalletTopUpRequestId())
                || request.getRequestedAmount() != payment.getBaseAmount()
                || !request.getCurrency().equalsIgnoreCase(payment.getCurrency())) {
            throw new ManualPaymentInstructionConflictException("Wallet top-up payment target does not match");
        }
        if (paymentRepository.existsApprovedPaymentForWalletTopUpRequest(request.getId())) {
            throw new ManualPaymentInstructionConflictException("Wallet top-up already has an approved payment");
        }
    }

    private ManualPaymentDestination destination() {
        return destinationProvider.firstActiveDestination()
                .orElseThrow(ManualPaymentDestinationUnavailableException::new);
    }

    private static void validateCommand(InitializeManualCardPaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.instructionRequestId(), "instructionRequestId must not be null");
        Objects.requireNonNull(command.paymentId(), "paymentId must not be null");
        if (command.telegramUserId() == null || command.telegramUserId() <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
    }
}
