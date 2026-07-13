package com.parazit.panel.application.purchase.result;

import com.parazit.panel.application.payment.manual.result.ManualCardPaymentInstructionResult;
import java.util.UUID;

public record PurchaseManualPaymentResult(
        UUID purchaseSessionId,
        UUID orderId,
        ManualCardPaymentInstructionResult instruction
) {
}
