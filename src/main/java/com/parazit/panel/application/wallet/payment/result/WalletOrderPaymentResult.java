package com.parazit.panel.application.wallet.payment.result;

import com.parazit.panel.domain.order.Money;
import java.time.Instant;
import java.util.UUID;

public record WalletOrderPaymentResult(
        UUID orderId,
        UUID paymentId,
        UUID walletTransactionId,
        Money paidAmount,
        Money balanceBefore,
        Money balanceAfter,
        WalletOrderPaymentOutcome outcome,
        boolean replayed,
        Instant approvedAt
) {
}
