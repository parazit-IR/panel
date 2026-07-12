package com.parazit.panel.application.port.in.payment.manual;

import com.parazit.panel.application.payment.manual.command.InitializeManualCardPaymentCommand;
import com.parazit.panel.application.payment.manual.result.InitializeManualCardPaymentResult;

public interface InitializeManualCardPaymentUseCase {

    InitializeManualCardPaymentResult initialize(InitializeManualCardPaymentCommand command);
}
