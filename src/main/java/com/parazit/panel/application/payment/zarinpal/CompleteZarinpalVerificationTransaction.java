package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.payment.ApprovePaymentCommand;
import com.parazit.panel.application.payment.PaymentApprovalService;
import com.parazit.panel.application.payment.PaymentApprovalSource;
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
    private final PaymentApprovalService paymentApprovalService;
    private final SystemClockPort clock;

    public CompleteZarinpalVerificationTransaction(
            PaymentRepository paymentRepository,
            ZarinpalPaymentAttemptRepository attemptRepository,
            PaymentApprovalService paymentApprovalService,
            SystemClockPort clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.attemptRepository = Objects.requireNonNull(attemptRepository, "attemptRepository must not be null");
        this.paymentApprovalService = Objects.requireNonNull(paymentApprovalService, "paymentApprovalService must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public CompletedZarinpalVerification approve(UUID paymentId, UUID attemptId, ZarinpalVerifyResponse response) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        ZarinpalPaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        java.time.Instant now = clock.now();
        attempt.markVerified(
                response.referenceId(),
                String.valueOf(response.code()),
                response.cardHash(),
                response.cardPanMasked(),
                now
        );
        paymentApprovalService.approve(new ApprovePaymentCommand(
                payment.getId(),
                PaymentApprovalSource.ZARINPAL_VERIFICATION,
                response.referenceId(),
                attempt.getAuthority(),
                now
        ));
        attemptRepository.save(attempt);
        payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return new CompletedZarinpalVerification(payment, attempt);
    }

    @Transactional
    public CompletedZarinpalVerification reconcileApproved(UUID paymentId, UUID attemptId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        ZarinpalPaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        java.time.Instant approvedAt = payment.getApprovedAt() == null ? clock.now() : payment.getApprovedAt();
        paymentApprovalService.approve(new ApprovePaymentCommand(
                payment.getId(),
                PaymentApprovalSource.ZARINPAL_VERIFICATION,
                attempt.getReferenceId(),
                attempt.getAuthority(),
                approvedAt
        ));
        payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
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
