package com.parazit.panel.application.wallet.payment.result;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.order.OrderType;
import java.util.UUID;

public record WalletOrderPaymentPreviewResult(
        UUID orderId,
        OrderType orderType,
        Money orderAmount,
        Money walletBalance,
        Money projectedBalance,
        boolean walletPaymentAvailable,
        boolean sufficientBalance,
        WalletOrderPaymentOutcome unavailableOutcome
) {
}
