package com.parazit.panel.application.port.in.purchase;

import com.parazit.panel.application.purchase.result.PurchasePaymentMethodsResult;
import java.util.UUID;

public interface ContinuePurchaseToPaymentUseCase {

    PurchasePaymentMethodsResult continueToPayment(long telegramUserId, UUID purchaseSessionId);
}
