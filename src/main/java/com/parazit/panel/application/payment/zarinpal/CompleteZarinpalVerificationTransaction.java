package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.payment.PaymentNotFoundException;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyResponse;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import com.parazit.panel.domain.payment.zarinpal.repository.ZarinpalPaymentAttemptRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CompleteZarinpalVerificationTransaction {

    private final PaymentRepository paymentRepository;
    private final ZarinpalPaymentAttemptRepository attemptRepository;
    private final SystemClockPort clock;

    public CompleteZarinpalVerificationTransaction(
            PaymentRepository paymentRepository,
            ZarinpalPaymentAttemptRepository attemptRepository,
            SystemClockPort clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.attemptRepository = Objects.requireNonNull(attemptRepository, "attemptRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public CompletedZarinpalVerification approve(UUID paymentId, UUID attemptId, ZarinpalVerifyResponse response) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        ZarinpalPaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        if (paymentRepository.existsApprovedPaymentForOrder(payment.getOrderId())
                && payment.getStatus() != com.parazit.panel.domain.payment.PaymentStatus.APPROVED) {
            throw new PaymentVerificationConflictException("Order already has an approved payment");
        }
        attempt.markVerified(
                response.referenceId(),
                String.valueOf(response.code()),
                response.cardHash(),
                response.cardPanMasked(),
                clock.now()
        );
        payment.markApproved(clock.now(), response.referenceId(), attempt.getAuthority());
        paymentRepository.save(payment);
        attemptRepository.save(attempt);
        return new CompletedZarinpalVerification(payment, attempt);
    }

    @Transactional
    public CompletedZarinpalVerification fail(UUID paymentId, UUID attemptId, String code, String message) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        ZarinpalPaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.markFailed(code, message, clock.now());
        payment.markFailed(clock.now(), message);
        paymentRepository.save(payment);
        attemptRepository.save(attempt);
        return new CompletedZarinpalVerification(payment, attempt);
    }

    @Transactional
    public CompletedZarinpalVerification unknown(UUID paymentId, UUID attemptId, String code, String message) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        ZarinpalPaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.markUnknown(code, message, clock.now());
        payment.markUnknown(clock.now(), message);
        paymentRepository.save(payment);
        attemptRepository.save(attempt);
        return new CompletedZarinpalVerification(payment, attempt);
    }
}
