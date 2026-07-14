package com.parazit.panel.application.wallet.result;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.wallet.WalletTransactionDirection;
import com.parazit.panel.domain.wallet.WalletTransactionType;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionSummaryResult(
        UUID transactionId,
        WalletTransactionDirection direction,
        WalletTransactionType type,
        Money amount,
        Money balanceAfter,
        String descriptionCode,
        Instant occurredAt
) {
}
