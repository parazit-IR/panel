package com.parazit.panel.application.wallet.result;

import com.parazit.panel.domain.order.Money;
import com.parazit.panel.domain.wallet.WalletStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record CustomerWalletResult(
        UUID walletId,
        Money balance,
        WalletStatus status,
        long transactionCount,
        Optional<Instant> lastTransactionAt,
        boolean topUpAvailable,
        boolean walletPaymentAvailable
) {

    public CustomerWalletResult {
        lastTransactionAt = lastTransactionAt == null ? Optional.empty() : lastTransactionAt;
    }
}
