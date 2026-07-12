package com.parazit.panel.application.port.in.payment;

import com.parazit.panel.application.payment.command.CreatePaymentCommand;
import com.parazit.panel.application.payment.result.PaymentResult;

public interface CreatePaymentUseCase {

    PaymentResult create(CreatePaymentCommand command);
}
