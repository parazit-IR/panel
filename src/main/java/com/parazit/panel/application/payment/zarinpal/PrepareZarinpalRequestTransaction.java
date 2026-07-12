package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.payment.PaymentNotFoundException;
import com.parazit.panel.application.payment.PaymentOrderNotFoundException;
import com.parazit.panel.application.payment.zarinpal.command.InitializeZarinpalPaymentCommand;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalAttemptStatus;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import com.parazit.panel.domain.payment.zarinpal.repository.ZarinpalPaymentAttemptRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PrepareZarinpalRequestTransaction {

    private final PaymentRepository paymentRepository;
    private final ZarinpalPaymentAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ZarinpalAmountConverter amountConverter;
    private final SystemClockPort clock;

    public PrepareZarinpalRequestTransaction(
            PaymentRepository paymentRepository,
            ZarinpalPaymentAttemptRepository attemptRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            ZarinpalAmountConverter amountConverter,
            SystemClockPort clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.attemptRepository = Objects.requireNonNull(attemptRepository, "attemptRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.amountConverter = Objects.requireNonNull(amountConverter, "amountConverter must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public PreparedZarinpalRequest prepare(InitializeZarinpalPaymentCommand command) {
        validateCommand(command);
        return attemptRepository.findByRequestId(command.requestId())
                .map(existing -> replayOrResume(command, existing))
                .orElseGet(() -> createNewAttempt(command));
    }

    private PreparedZarinpalRequest replayOrResume(
            InitializeZarinpalPaymentCommand command,
            ZarinpalPaymentAttempt attempt
    ) {
        Payment payment = loadAndValidatePayment(command);
        if (!attempt.getPaymentId().equals(payment.getId())) {
            throw new PaymentVerificationConflictException("requestId belongs to a different payment");
        }
        if (attempt.getStatus() == ZarinpalAttemptStatus.REDIRECT_READY) {
            return new PreparedZarinpalRequest(payment, attempt, true);
        }
        if (attempt.getStatus() == ZarinpalAttemptStatus.REQUESTING || attempt.getStatus() == ZarinpalAttemptStatus.UNKNOWN) {
            return new PreparedZarinpalRequest(payment, attempt, false);
        }
        throw new ZarinpalPaymentNotAllowedException("Zarinpal request cannot be replayed with attempt status " + attempt.getStatus());
    }

    private PreparedZarinpalRequest createNewAttempt(InitializeZarinpalPaymentCommand command) {
        Payment payment = loadAndValidatePayment(command);
        long gatewayAmount = amountConverter.toGatewayAmount(payment.getPayableAmount(), payment.getCurrency());
        ZarinpalPaymentAttempt attempt = ZarinpalPaymentAttempt.create(payment.getId(), command.requestId(), gatewayAmount);
        attempt.markRequesting(clock.now());
        return new PreparedZarinpalRequest(payment, attemptRepository.save(attempt), false);
    }

    private Payment loadAndValidatePayment(InitializeZarinpalPaymentCommand command) {
        User user = userRepository.findByTelegramUserId(command.telegramUserId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        Payment payment = paymentRepository.findById(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        if (!payment.getUserId().equals(user.getId())) {
            throw new PaymentNotFoundException(command.paymentId());
        }
        if (payment.getMethod() != PaymentMethod.ZARINPAL) {
            throw new ZarinpalPaymentNotAllowedException("Payment method is not ZARINPAL");
        }
        if (payment.getStatus() == PaymentStatus.APPROVED) {
            throw new PaymentAlreadyApprovedException();
        }
        if (payment.isTerminal()) {
            throw new ZarinpalPaymentNotAllowedException("Payment is not payable with status " + payment.getStatus());
        }
        if (payment.getPayableAmount() <= 0) {
            throw new ZarinpalPaymentNotAllowedException("Payment amount must be positive");
        }
        Order order = orderRepository.findById(payment.getOrderId())
                .orElseThrow(() -> new PaymentOrderNotFoundException(payment.getOrderId()));
        if (!order.getUserId().equals(user.getId())) {
            throw new PaymentVerificationConflictException("Order owner does not match payment owner");
        }
        if (paymentRepository.existsApprovedPaymentForOrder(order.getId())) {
            throw new PaymentVerificationConflictException("Order already has an approved payment");
        }
        Instant now = clock.now();
        if (!payment.getExpiresAt().isAfter(now)) {
            throw new ZarinpalPaymentNotAllowedException("Payment is expired");
        }
        return payment;
    }

    private void validateCommand(InitializeZarinpalPaymentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.requestId(), "requestId must not be null");
        Objects.requireNonNull(command.paymentId(), "paymentId must not be null");
        if (command.telegramUserId() == null || command.telegramUserId() <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        if (command.description() == null || command.description().trim().isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (command.description().trim().length() > 500) {
            throw new IllegalArgumentException("description must be at most 500 characters");
        }
    }
}
