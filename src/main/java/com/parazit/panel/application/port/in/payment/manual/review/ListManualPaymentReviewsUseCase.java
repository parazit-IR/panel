package com.parazit.panel.application.port.in.payment.manual.review;

import com.parazit.panel.application.payment.manual.review.query.ListManualPaymentReviewsQuery;
import com.parazit.panel.application.payment.manual.review.result.ManualPaymentReviewResult;
import java.util.List;

public interface ListManualPaymentReviewsUseCase {

    List<ManualPaymentReviewResult> list(ListManualPaymentReviewsQuery query);
}
