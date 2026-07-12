package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.payment.PaymentNotFoundException;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateResponse;
import com.parazit.panel.domain.payment.Payment;
import com.parazit.panel.domain.payment.repository.PaymentRepository;
import com.parazit.panel.domain.payment.zarinpal.ZarinpalPaymentAttempt;
import com.parazit.panel.domain.payment.zarinpal.repository.ZarinpalPaymentAttemptRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CompleteZarinpalRequestTransaction {

    private final PaymentRepository paymentRepository;
    private final ZarinpalPaymentAttemptRepository attemptRepository;
    private final SystemClockPort clock;

    public CompleteZarinpalRequestTransaction(
            PaymentRepository paymentRepository,
            ZarinpalPaymentAttemptRepository attemptRepository,
            SystemClockPort clock
    ) {
        this.paymentRepository = Objects.requireNonNull(paymentRepository, "paymentRepository must not be null");
        this.attemptRepository = Objects.requireNonNull(attemptRepository, "attemptRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Transactional
    public ZarinpalPaymentAttempt markReady(UUID paymentId, UUID attemptId, ZarinpalCreateResponse response) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        ZarinpalPaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.markRedirectReady(String.valueOf(response.authority()), clock.now(), String.valueOf(response.code()));
        payment.markWaitingForPayment();
        paymentRepository.save(payment);
        return attemptRepository.save(attempt);
    }

    @Transactional
    public ZarinpalPaymentAttempt markFailed(UUID attemptId, String code, String message) {
        ZarinpalPaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.markFailed(code, message, clock.now());
        return attemptRepository.save(attempt);
    }

    @Transactional
    public ZarinpalPaymentAttempt markUnknown(UUID attemptId, String code, String message) {
        ZarinpalPaymentAttempt attempt = attemptRepository.findById(attemptId).orElseThrow();
        attempt.markUnknown(code, message, clock.now());
        return attemptRepository.save(attempt);
    }
}
