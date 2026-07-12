package com.parazit.panel.application.port.in.payment;

import com.parazit.panel.application.payment.result.PaymentResult;
import java.util.List;
import java.util.UUID;

public interface GetPaymentUseCase {

    PaymentResult getById(UUID paymentId);

    List<PaymentResult> listByOrderId(UUID orderId);
}
