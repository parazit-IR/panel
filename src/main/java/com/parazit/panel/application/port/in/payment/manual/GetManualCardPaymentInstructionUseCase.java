package com.parazit.panel.application.port.in.payment.manual;

import com.parazit.panel.application.payment.manual.query.GetManualCardPaymentInstructionQuery;
import com.parazit.panel.application.payment.manual.result.ManualCardPaymentInstructionResult;

public interface GetManualCardPaymentInstructionUseCase {

    ManualCardPaymentInstructionResult getCurrent(GetManualCardPaymentInstructionQuery query);
}
