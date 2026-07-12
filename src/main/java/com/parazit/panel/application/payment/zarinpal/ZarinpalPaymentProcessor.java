package com.parazit.panel.application.payment.zarinpal;

import com.parazit.panel.application.payment.command.PaymentInitializationCommand;
import com.parazit.panel.application.payment.command.PaymentVerificationCommand;
import com.parazit.panel.application.payment.result.PaymentInitializationResult;
import com.parazit.panel.application.payment.result.PaymentVerificationResult;
import com.parazit.panel.application.port.out.payment.PaymentProcessor;
import com.parazit.panel.domain.payment.PaymentMethod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.payment.zarinpal", name = "enabled", havingValue = "true")
public class ZarinpalPaymentProcessor implements PaymentProcessor {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.ZARINPAL;
    }

    @Override
    public PaymentInitializationResult initiate(PaymentInitializationCommand command) {
        throw new UnsupportedOperationException("Use InitializeZarinpalPaymentUseCase for Zarinpal initialization");
    }

    @Override
    public PaymentVerificationResult verify(PaymentVerificationCommand command) {
        throw new UnsupportedOperationException("Use HandleZarinpalCallbackUseCase for Zarinpal verification");
    }
}
