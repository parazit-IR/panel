package com.parazit.panel.application.port.in.payment.zarinpal;

import com.parazit.panel.application.payment.zarinpal.command.InitializeZarinpalPaymentCommand;
import com.parazit.panel.application.payment.zarinpal.result.InitializeZarinpalPaymentResult;

public interface InitializeZarinpalPaymentUseCase {

    InitializeZarinpalPaymentResult initialize(InitializeZarinpalPaymentCommand command);
}
