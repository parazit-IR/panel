package com.parazit.panel.application.port.in.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.query.GetManualPaymentReceiptContentQuery;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReceiptContentResult;

public interface GetManualPaymentReceiptContentUseCase {
    ManualPaymentReceiptContentResult getContent(GetManualPaymentReceiptContentQuery query);
}
