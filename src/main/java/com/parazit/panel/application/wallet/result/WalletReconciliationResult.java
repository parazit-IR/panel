package com.parazit.panel.application.wallet.result;

import com.parazit.panel.domain.order.Money;
import java.util.UUID;

public record WalletReconciliationResult(
        UUID walletId,
        Money storedBalance,
        Money ledgerCalculatedBalance,
        boolean consistent,
        long transactionCount
) {
}
