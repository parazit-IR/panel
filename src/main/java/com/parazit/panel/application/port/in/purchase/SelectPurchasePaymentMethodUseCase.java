package com.parazit.panel.application.port.in.purchase;

import com.parazit.panel.application.purchase.result.PurchaseManualPaymentResult;
import com.parazit.panel.application.purchase.result.PurchaseOnlinePaymentResult;
import java.util.UUID;

public interface SelectPurchasePaymentMethodUseCase {

    PurchaseManualPaymentResult selectManual(long telegramUserId, UUID purchaseSessionId);

    PurchaseOnlinePaymentResult selectOnline(long telegramUserId, UUID purchaseSessionId);
}
