package com.parazit.panel.application.port.in.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.command.ReleaseManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;

public interface ReleaseManualPaymentReviewUseCase {

    ManualPaymentReviewResult release(ReleaseManualPaymentReviewCommand command);
}
