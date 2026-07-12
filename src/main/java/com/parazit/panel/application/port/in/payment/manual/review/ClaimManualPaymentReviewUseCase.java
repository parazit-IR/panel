package com.parazit.panel.application.port.in.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.command.ClaimManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;

public interface ClaimManualPaymentReviewUseCase {

    ManualPaymentReviewResult claim(ClaimManualPaymentReviewCommand command);
}
