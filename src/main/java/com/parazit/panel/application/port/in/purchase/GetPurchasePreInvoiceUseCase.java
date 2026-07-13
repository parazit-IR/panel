package com.parazit.panel.application.port.in.purchase;

import com.parazit.panel.application.purchase.result.PurchasePreInvoiceResult;
import java.util.UUID;

public interface GetPurchasePreInvoiceUseCase {

    PurchasePreInvoiceResult get(long telegramUserId, UUID purchaseSessionId);
}
