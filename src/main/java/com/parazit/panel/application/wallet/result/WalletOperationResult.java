package com.parazit.panel.application.wallet.result;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.wallet.WalletOperationOutcome;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import java.time.Instant;
import java.util.UUID;

public record WalletOperationResult(
        UUID walletId,
        UUID transactionId,
        Money balanceBefore,
        Money balanceAfter,
        Money amount,
        WalletTransactionDirection direction,
        WalletOperationOutcome outcome,
        boolean replayed,
        Instant occurredAt
) {
}
