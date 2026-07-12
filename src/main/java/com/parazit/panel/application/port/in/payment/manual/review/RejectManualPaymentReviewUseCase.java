package com.parazit.panel.application.port.in.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.command.RejectManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;

public interface RejectManualPaymentReviewUseCase {

    ManualPaymentReviewResult reject(RejectManualPaymentReviewCommand command);
}
