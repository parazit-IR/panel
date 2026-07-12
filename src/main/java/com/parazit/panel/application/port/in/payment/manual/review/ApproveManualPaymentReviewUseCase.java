package com.parazit.panel.application.port.in.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.command.ApproveManualPaymentReviewCommand;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;

public interface ApproveManualPaymentReviewUseCase {

    ManualPaymentReviewResult approve(ApproveManualPaymentReviewCommand command);
}
