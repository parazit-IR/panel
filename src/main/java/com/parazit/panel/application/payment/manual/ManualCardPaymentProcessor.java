package com.parazit.panel.application.payment.manual;

import com.parazit.panel.application.payment.command.PaymentInitializationCommand;
import com.parazit.panel.application.payment.command.PaymentVerificationCommand;
import com.parazit.panel.application.payment.result.PaymentInitializationResult;
import com.parazit.panel.application.payment.result.PaymentVerificationResult;
import com.parazit.panel.application.port.out.payment.PaymentProcessor;
import com.parazit.panel.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class ManualCardPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.CARD_TO_CARD;
    }

    @Override
    public PaymentInitializationResult initiate(PaymentInitializationCommand command) {
        throw new UnsupportedOperationException("Use InitializeManualCardPaymentUseCase for manual card payment");
    }

    @Override
    public PaymentVerificationResult verify(PaymentVerificationCommand command) {
        throw new UnsupportedOperationException("Manual card payment verification is deferred to operator review");
    }
}
