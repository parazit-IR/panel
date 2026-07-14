package com.parazit.panel.application.wallet.topup.result;

import com.parazit.panel.application.wallet.topup.WalletTopUpApprovalOutcome;
import com.parazit.panel.domain.order.Money;
import java.time.Instant;
import java.util.UUID;

public record WalletTopUpApprovalResult(
        UUID topUpRequestId,
        UUID paymentId,
        UUID walletTransactionId,
        Money creditedAmount,
        Money balanceAfter,
        WalletTopUpApprovalOutcome outcome,
        boolean replayed,
        Instant creditedAt
) {
}
