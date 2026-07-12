package com.parazit.panel.application.port.in.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.command.SubmitManualPaymentReceiptCommand;
import com.parazit.panel.application.payment.manual.receipt.result.SubmitManualPaymentReceiptResult;

public interface SubmitManualPaymentReceiptUseCase {
    SubmitManualPaymentReceiptResult submit(SubmitManualPaymentReceiptCommand command);
}
