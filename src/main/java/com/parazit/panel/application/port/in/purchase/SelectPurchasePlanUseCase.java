package com.parazit.panel.application.port.in.purchase;

import com.parazit.panel.application.purchase.result.PurchasePreInvoiceResult;
import java.util.UUID;

public interface SelectPurchasePlanUseCase {

    PurchasePreInvoiceResult select(long telegramUserId, UUID planId);
}
