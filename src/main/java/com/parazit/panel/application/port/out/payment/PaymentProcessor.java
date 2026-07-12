package com.parazit.panel.application.port.out.payment;

import com.parazit.panel.application.payment.command.PaymentInitializationCommand;
import com.parazit.panel.application.payment.command.PaymentVerificationCommand;
import com.parazit.panel.application.payment.result.PaymentInitializationResult;
import com.parazit.panel.application.payment.result.PaymentVerificationResult;
import com.parazit.panel.domain.payment.PaymentMethod;

public interface PaymentProcessor {

    PaymentMethod supportedMethod();

    PaymentInitializationResult initiate(PaymentInitializationCommand command);

    PaymentVerificationResult verify(PaymentVerificationCommand command);
}
