package com.parazit.panel.application.port.in.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.query.GetManualPaymentReviewQuery;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;

public interface GetManualPaymentReviewUseCase {

    ManualPaymentReviewResult get(GetManualPaymentReviewQuery query);
}
