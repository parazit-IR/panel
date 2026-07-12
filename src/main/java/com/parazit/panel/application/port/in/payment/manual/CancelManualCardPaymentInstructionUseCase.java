package com.parazit.panel.application.port.in.payment.manual;

import com.parazit.panel.application.payment.manual.command.CancelManualCardPaymentInstructionCommand;
import com.parazit.panel.application.payment.manual.result.ManualCardPaymentInstructionResult;

public interface CancelManualCardPaymentInstructionUseCase {

    ManualCardPaymentInstructionResult cancel(CancelManualCardPaymentInstructionCommand command);
}
