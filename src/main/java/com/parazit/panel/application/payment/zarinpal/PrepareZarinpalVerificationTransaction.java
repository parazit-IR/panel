package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.payment.PaymentNotFoundException;
import com.parazit.panel.application.payment.zarinpal.command.HandleZarinpalCallbackCommand;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.PaymentMethod;
import com.parazit.panel.domain.payment.PaymentStatus;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalAttemptStatus;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import com.parazit.panel.domain.payment.zarinpal.repository.ZarinpalPaymentAttemptRepository;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PrepareZarinpalVerificationTransaction {

    private final PaymentRepository paymentRepository;
    private final ZarinpalPaymentAttemptRepository attemptRepository;
    private final SystemClockPort clock;

    public PrepareZarinpalVerificationTransaction(
            PaymentRepository paymentRepository,
            ZarinpalPaymentAttemptRepository attemptRepository,
            SystemClockPort clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.attemptRepository = Objects.requireNonNull(attemptRepository, "attemptRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public PreparedZarinpalVerification prepare(HandleZarinpalCallbackCommand command) {
        validate(command);
        String authority = ZarinpalPaymentAttempt.normalizeAuthority(command.authority());
        ZarinpalPaymentAttempt attempt = attemptRepository.findByAuthority(authority)
                .orElseThrow(ZarinpalAuthorityNotFoundException::new);
        Payment payment = paymentRepository.findById(attempt.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(attempt.getPaymentId()));
        validatePayment(payment);

        if (payment.getStatus() == PaymentStatus.APPROVED || attempt.getStatus() == ZarinpalAttemptStatus.VERIFIED) {
            return new PreparedZarinpalVerification(payment, attempt, true, false);
        }
        if ("NOK".equals(normalizeStatus(command.callbackStatus()))) {
            attempt.markCancelled(clock.now());
            payment.markCancelled(clock.now());
            attemptRepository.save(attempt);
            paymentRepository.save(payment);
            return new PreparedZarinpalVerification(payment, attempt, false, true);
        }
        if (!"OK".equals(normalizeStatus(command.callbackStatus()))) {
            throw new ZarinpalCallbackInvalidException("Unsupported Zarinpal callback status");
        }

        attempt.markCallbackReceived(clock.now());
        attempt.markVerifying(clock.now());
        if (payment.getStatus() == PaymentStatus.UNKNOWN) {
            payment.resumeProcessingFromUnknown();
        } else {
            payment.markProcessing();
        }
        attemptRepository.save(attempt);
        paymentRepository.save(payment);
        return new PreparedZarinpalVerification(payment, attempt, false, false);
    }

    private void validatePayment(Payment payment) {
        if (payment.getMethod() != PaymentMethod.ZARINPAL) {
            throw new PaymentVerificationConflictException("Payment method is not ZARINPAL");
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED
                || payment.getStatus() == PaymentStatus.REJECTED
                || payment.getStatus() == PaymentStatus.EXPIRED
                || payment.getStatus() == PaymentStatus.FAILED) {
            throw new ZarinpalPaymentNotAllowedException("Payment cannot be verified with status " + payment.getStatus());
        }
    }

    private void validate(HandleZarinpalCallbackCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ZarinpalPaymentAttempt.normalizeAuthority(command.authority());
        if (command.callbackStatus() == null || command.callbackStatus().trim().isBlank()) {
            throw new ZarinpalCallbackInvalidException("Zarinpal callback status is required");
        }
    }

    static String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }
}
