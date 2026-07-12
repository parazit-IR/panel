package com.parazit.panel.application.port.in.payment.manual.receipt;

import com.parazit.panel.application.payment.manual.receipt.query.ListManualPaymentReviewQueueQuery;
import com.parazit.panel.application.payment.manual.receipt.result.ManualPaymentReviewQueueItemResult;
import java.util.List;

public interface ListManualPaymentReviewQueueUseCase {
    List<ManualPaymentReviewQueueItemResult> list(ListManualPaymentReviewQueueQuery query);
}
