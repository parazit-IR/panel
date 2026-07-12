package com.parazit.panel.application.port.in.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.query.GetManualPaymentReceiptByIdQuery;
import com.parazit.panel.application.payment.manual.receipt.query.GetManualPaymentReceiptByPaymentQuery;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReceiptResult;

public interface GetManualPaymentReceiptUseCase {
    ManualPaymentReceiptResult getById(GetManualPaymentReceiptByIdQuery query);

    ManualPaymentReceiptResult getCurrentByPayment(GetManualPaymentReceiptByPaymentQuery query);
}
